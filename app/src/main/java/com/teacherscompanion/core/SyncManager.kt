package com.teacherscompanion.core

import com.teacherscompanion.data.local.dao.PendingSyncDao
import com.teacherscompanion.data.local.entity.PendingSyncEntity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val pendingSyncDao: PendingSyncDao,
    private val supabaseClient: SupabaseClient,
    private val networkMonitor: NetworkMonitor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var isSyncing = false

    fun start() {
        scope.launch {
            networkMonitor.isOnline.collectLatest { online ->
                if (online) processPendingSyncs()
            }
        }
    }

    fun queueSync(entityType: String, entityId: String, action: String, payload: String? = null) {
        scope.launch {
            pendingSyncDao.insert(
                PendingSyncEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = entityType,
                    entityId = entityId,
                    action = action,
                    payload = payload
                )
            )
            if (networkMonitor.isOnline.value) processPendingSyncs()
        }
    }

    private suspend fun processPendingSyncs() {
        if (isSyncing) return
        isSyncing = true
        try {
            val pending = pendingSyncDao.getAllPending()
            for (item in pending) {
                try {
                    processItem(item)
                    pendingSyncDao.delete(item.id)
                } catch (e: Exception) {
                    if (item.retryCount >= 3) pendingSyncDao.delete(item.id)
                    else pendingSyncDao.insert(item.copy(retryCount = item.retryCount + 1))
                    break
                }
            }
        } finally { isSyncing = false }
    }

    private suspend fun processItem(item: PendingSyncEntity) {
        val table = entityTypeToTable(item.entityType) ?: return
        val payloadData = item.payload?.let { json.parseToJsonElement(it) }

        when (item.action) {
            "insert" -> {
                if (payloadData != null) {
                    supabaseClient.from(table).insert(payloadData)
                }
            }
            "update" -> {
                if (payloadData != null) {
                    supabaseClient.from(table).update(payloadData) {
                        eq("id", item.entityId)
                    }
                }
            }
            "delete" -> {
                supabaseClient.from(table).update(mapOf("deleted_at" to java.time.Instant.now().toString())) {
                    eq("id", item.entityId)
                }
            }
        }
    }

    private fun entityTypeToTable(type: String): String? = when (type) {
        "school" -> "schools"
        "school_class" -> "school_classes"
        "subject" -> "subjects"
        "syllabus_topic" -> "syllabus_topics"
        "lesson_note" -> "lesson_notes"
        "question" -> "questions"
        "profile" -> "profiles"
        "alarm" -> "alarms"
        "period_reminder" -> "period_reminders"
        else -> null
    }
}
