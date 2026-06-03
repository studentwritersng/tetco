package com.teacherscompanion.core

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.realtime.Realtime

object SupabaseClientProvider {
    private const val SUPABASE_URL = "https://yovqrtevqxkcfxvifzxv.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlvdnFydGV2cXhrY2Z4dmlmenh2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODAzMTY2ODIsImV4cCI6MjA5NTg5MjY4Mn0.iMoQhnImuZxoyortugovRDu92zeQ2weocT0USTGV3A0"

    val client: SupabaseClient by lazy {
        createSupabaseClient(SUPABASE_URL, SUPABASE_ANON_KEY) {
            install(Auth)
            install(Postgrest)
            install(Storage)
            install(Functions)
            install(Realtime)
            // engine auto-detected
        }
    }
}
