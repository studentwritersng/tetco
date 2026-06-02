import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!
    );

    const authHeader = req.headers.get("authorization");
    if (!authHeader) throw new Error("No authorization header");

    const { data: { user } } = await supabase.auth.getUser(authHeader.replace("Bearer ", ""));
    if (!user) throw new Error("Invalid token");

    const { topic_id, topic_title, subject_name, class_name, term, week_number } = await req.json();

    const month = new Date().toISOString().slice(0, 7);

    const { data: usage } = await supabase
      .from("ai_usage")
      .select("lesson_notes_generated")
      .eq("teacher_id", user.id)
      .eq("month", month)
      .single();

    const { data: profile } = await supabase
      .from("profiles")
      .select("plan")
      .eq("id", user.id)
      .single();

    const { data: planLimits } = await supabase
      .from("plans")
      .select("lesson_note_limit")
      .eq("name", profile?.plan === "advanced" ? "Advanced" : profile?.plan === "premium" ? "Premium" : "Basic")
      .single();

    const currentUsage = usage?.lesson_notes_generated || 0;
    const limit = planLimits?.lesson_note_limit;
    if (limit !== null && limit !== undefined && currentUsage >= limit) {
      return new Response(
        JSON.stringify({ error: "QUOTA_EXCEEDED", limit }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 403 }
      );
    }

    const openrouterKey = Deno.env.get("OPENROUTER_API_KEY");
    if (!openrouterKey) throw new Error("AI service configuration missing (OPENROUTER_API_KEY)");

    const openrouterModel = Deno.env.get("OPENROUTER_MODEL") || "openai/gpt-4o-mini";

    const classLevelGuidance = getClassLevelGuidance(class_name);

    const today = new Date();
    const dateStr = today.toLocaleDateString("en-GB", {
      day: "numeric", month: "long", year: "numeric"
    });

    const prompt = `You are an experienced Nigerian ${class_name} teacher. Write a complete, well-structured lesson note following the exact format below. Use BRITISH ENGLISH spelling, grammar, and constructions throughout (e.g., "colour" not "color", "centre" not "center", "organise" not "organize", "behaviour" not "behavior"). When examples are needed, use Nigerian examples from Nigerian life, culture, and environment unless a non-Nigerian example is essential.

${classLevelGuidance}

PARAMETERS:
Subject: ${subject_name}
Class: ${class_name}
Term: ${term} Term
Week: ${week_number}
Topic: ${topic_title}
Date: ${dateStr}
Duration: 60 minutes

FORMAT — follow this structure EXACTLY, including all section headers:

---

Subject: ${subject_name}
Class: ${class_name}
Week: ${week_number}
Period: 1
Topic: ${topic_title}
Duration: 60 minutes
Theme / Aspect: [derive a relevant theme from the topic]
Date: ${dateStr}

Instructional Materials:
[List 3-4 practical teaching aids relevant to the topic and a Nigerian classroom environment. Each on a new line starting with a dash.]

Reference Books:
[List 2 Nigerian-relevant reference texts with plausible author names and titles. Use citation format: "Book Title" by Author Name (Cited as: Ref:ShortName).]

Behavioural Objectives:
By the end of the lesson, students should be able to:
[List 3-5 measurable, specific learning objectives. Each starting with an action verb and on a new line with a dash.]

Previous Knowledge:
[1-2 sentences describing what students already know that connects to this topic. Reference prior learning from earlier classes or terms.]

Introduction:
[2-3 sentences describing how the teacher sets the stage and engages students. Include the teaching method used.]

Presentation (Steps):
[Provide 4 clear steps. Each step MUST have both Teacher's Activity and Student's Activity.]

Step 1 - Teacher's Activity: [What the teacher does]
        Student's Activity: [What the students do in response]

Step 2 - Teacher's Activity: [What the teacher does]
        Student's Activity: [What the students do in response]

Step 3 - Teacher's Activity: [What the teacher does]
        Student's Activity: [What the students do in response]

Step 4 - Teacher's Activity: [What the teacher does]
        Student's Activity: [What the students do in response]

Content:
[Write 4-6 substantial paragraphs organised under clear subheadings. This is the main body of knowledge the teacher delivers. Include definitions, explanations, classifications, examples, and citations to the reference books where appropriate. Use [Ref:ShortName] for citations. Subheadings should be in UPPERCASE.]

Evaluation:
[Write 3-4 questions that assess understanding of the lesson objectives. Number each question.]

Assignment:
[Write 1-2 meaningful take-home tasks that reinforce the lesson. Use Nigerian contexts.]

Teacher's Name: _________________
Signature: _______________  Date: _______________
HOD's Name: _________________
Signature: _______________  Date: _______________

---

IMPORTANT INSTRUCTIONS:
- Use BRITISH ENGLISH spelling and grammar exclusively
- ALL examples, names, places, foods, and contexts MUST be Nigerian (e.g., Lagos, Abuja, NAFDAC, JAMB, suya, jollof rice, Nollywood, Super Eagles, naira) unless a non-Nigerian example is strictly essential
- Follow the CLASS LEVEL GUIDANCE above precisely — it determines vocabulary, sentence structure, and conceptual depth
- Write in clear, instructional English suitable for a classroom setting
- Reference citations should appear as [Ref:ShortName] inline within the Content section
- Do NOT wrap the output in code fences or markdown — output ONLY the lesson note text`;

    const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${openrouterKey}`,
        "Content-Type": "application/json",
        "HTTP-Referer": "https://teacherscompanion.app",
        "X-Title": "Teacher's Companion"
      },
      body: JSON.stringify({
        model: openrouterModel,
        messages: [{ role: "user", content: prompt }],
        max_tokens: 4000
      })
    });

    const data = await response.json();
    if (!response.ok) throw new Error(data.error?.message || "OpenRouter generation failed");

    const content = data.choices?.[0]?.message?.content || "";
    const promptTokens = data.usage?.prompt_tokens || 0;
    const completionTokens = data.usage?.completion_tokens || 0;
    const totalTokens = promptTokens + completionTokens;

    await supabase
      .from("ai_usage")
      .upsert({
        teacher_id: user.id,
        month,
        lesson_notes_generated: currentUsage + 1,
        updated_at: new Date().toISOString(),
      }, { onConflict: "teacher_id,month" });

    await supabase.from("ai_usage_logs").insert({
      teacher_id: user.id,
      feature: "lesson_note",
      tokens_used: totalTokens,
    });

    if (topic_id) {
      const { data: existing } = await supabase
        .from("lesson_notes")
        .select("id")
        .eq("syllabus_topic_id", topic_id)
        .is("deleted_at", null)
        .single();

      if (existing) {
        await supabase
          .from("lesson_notes")
          .update({ content, ai_generated: true, updated_at: new Date().toISOString() })
          .eq("id", existing.id);
      } else {
        await supabase.from("lesson_notes").insert({
          syllabus_topic_id: topic_id,
          teacher_id: user.id,
          content,
          ai_generated: true,
        });
      }
    }

    return new Response(
      JSON.stringify({ content, tokens_used: completionTokens }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (error) {
    return new Response(
      JSON.stringify({ error: error.message }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 500 }
    );
  }
});

function getClassLevelGuidance(className: string): string {
  const lower = className.toLowerCase();

  // ---- PRIMARY (1-6) ----
  if (lower.startsWith("primary")) {
    const level = parseInt(className.replace(/\D/g, "")) || 0;
    if (level <= 3) {
      return `== CLASS LEVEL GUIDANCE — PRIMARY ${level} (LOWER PRIMARY) ==
Vocabulary: Use very simple, concrete vocabulary — everyday words a young child knows (house, water, tree, sun, mother, book, ball, food). Introduce ONE new subject-specific term at a time and define it immediately in the simplest possible words. Avoid abstract nouns, technical jargon, and multiple new terms in the same lesson.
Sentences: Keep sentences SHORT — 8 to 12 words maximum per sentence. Use simple present tense predominantly. Break instructions into single-step commands. Repeat key ideas in different ways.
Examples: Draw EVERY example from a young Nigerian child's immediate world: home, family, school, the local market, the village, familiar animals (goat, chicken, cow), common foods (rice, yam, beans, bread), games children play (ampe, ten-ten, hide-and-seek, football). Use characters with Nigerian names (Ada, Chidi, Bisi, Tunde, Ngozi). No abstract or unfamiliar scenarios.
Concepts: Present every idea as a concrete, observable fact. Avoid "why" questions that require abstract reasoning. Use "what", "where", "when", and "how many". Teach through stories, songs, rhymes, repetition, and physical activity. Every concept must be visible or tangible.
Activities: Suggest activities that involve drawing, colouring, matching, pointing, singing, acting out, or manipulating physical objects. Worksheets should have pictures, not just words.
Assessment: Use oral questions, show-and-tell, matching exercises, true/false with pictures, and simple fill-in-the-blank with a word bank. Do NOT expect written paragraphs.`;
    } else {
      return `== CLASS LEVEL GUIDANCE — PRIMARY ${level} (UPPER PRIMARY) ==
Vocabulary: Use concrete vocabulary with occasional introduction of simple subject-specific terms. Define each new term in parentheses the first time it appears. Avoid multiple layers of abstraction. Build on the simple vocabulary children already know from lower primary.
Sentences: Sentences of 10 to 15 words. Mix simple and compound sentences using "and", "but", "because". Use present tense primarily but occasionally past tense for narratives. Give 2-step instructions.
Examples: Draw examples from the Nigerian child's expanding world: the neighbourhood, local government area, the city or town they live in, Nigerian holidays and festivals, basic Nigerian industries (farming, trading, transport), familiar professions (teacher, nurse, farmer, trader, driver). Use Nigerian names and settings exclusively.
Concepts: Present concepts as facts with simple explanations. Introduce basic "why" and "how" questions when the answer can be observed or demonstrated. Begin simple classification, comparison, and sequencing. Connect new ideas to what students already know using explicit bridging statements.
Activities: Suggest group work, simple experiments or demonstrations, guided discovery, short writing tasks (2-3 sentences), filling in tables or charts, drawing and labelling, simple research (ask your parents), and short oral presentations.
Assessment: Use short-answer questions, fill-in-the-blank, matching, simple true/false with correction, listing, labelling diagrams, and writing 3-5 sentence paragraphs. Questions should test recall and basic comprehension.`;
    }
  }

  // ---- JUNIOR SECONDARY (JSS 1-3) ----
  if (lower.startsWith("jss") || lower.includes("junior secondary")) {
    const level = parseInt(className.replace(/\D/g, "")) || 0;
    return `== CLASS LEVEL GUIDANCE — JSS ${level} (JUNIOR SECONDARY) ==
Vocabulary: Use a moderate vocabulary with subject-specific terminology. Define technical terms clearly when introduced, but you may use them freely within the same lesson once defined. Students are building academic vocabulary across subjects.
Sentences: Sentences of 12 to 20 words. Vary sentence structure — simple, compound, and complex sentences. Use appropriate tenses including present, past, and future. Give multi-step instructions (3-4 steps). Use transition words (however, therefore, meanwhile, furthermore, consequently).
Examples: Draw examples from Nigerian national life: cities (Lagos, Abuja, Kano, Port Harcourt, Enugu, Ibadan), Nigerian institutions (NAFDAC, JAMB, NNPC, NITDA, NAFDAC, FRSC, NTA), Nigerian industries (banking, telecoms, agriculture, entertainment, technology), current events, and Nigerian popular culture (Nollywood, Afrobeats, Nigerian football). Use Nigerian professional contexts and real-world scenarios students may encounter.
Concepts: Present concepts with both definitions AND explanations of why they matter. Encourage basic analytical thinking: compare and contrast, classify, identify causes and effects, explain processes. Link concepts to students' personal experiences and observations. Introduce basic theoretical frameworks.
Activities: Suggest group discussions, debates, simple research projects, presentations, experiments (with basic lab equipment or household alternatives), report writing, data collection and simple analysis, case studies, and role-play of real-world Nigerian scenarios.
Assessment: Use questions requiring short paragraphs (5-8 sentences), explain-in-your-own-words prompts, compare-and-contrast, cause-and-effect, basic data interpretation (tables, charts, graphs), and application of concepts to new situations. Prepare students for the transition to senior secondary rigour.`;
  }

  // ---- SENIOR SECONDARY (SSS 1-3) ----
  if (lower.startsWith("sss") || lower.startsWith("s.s.s") || lower.includes("senior secondary")) {
    const level = parseInt(className.replace(/\D/g, "")) || 0;
    return `== CLASS LEVEL GUIDANCE — SSS ${level} (SENIOR SECONDARY) ==
Vocabulary: Full academic vocabulary with subject-specific terminology expected. Students should encounter and use technical terms fluently. Introduce advanced vocabulary and expect students to incorporate it in their own responses. Prepare students for the language demands of WAEC/SSCE, JAMB/UTME, and tertiary education.
Sentences: Full range of sentence structures — simple, compound, complex, and compound-complex. Sentences may be 15 to 30 words. Use sophisticated transitions and cohesive devices (nonetheless, notwithstanding, consequently, in contrast, similarly, subsequently, with reference to, in light of). Academic register is appropriate.
Examples: Draw examples from Nigerian and global contexts: Nigerian economic policy, public health challenges, environmental issues, technological innovation, political developments, legal frameworks, and cultural production. Also reference global examples where relevant (but always connect back to Nigerian realities). Use case studies from Nigerian businesses, government agencies, research institutions, and current affairs.
Concepts: Present concepts with theoretical depth, critical analysis, and multiple perspectives. Encourage students to evaluate, critique, synthesise, justify, and create. Connect topics to WAEC/SSCE examination expectations. Introduce scholarly debates and unresolved questions in the field. Expect students to form and defend evidence-based positions.
Activities: Suggest extended research projects, essays with argumentation, laboratory experiments with full scientific write-ups, independent investigation, seminar-style presentations, critical analysis of primary and secondary sources, problem-based learning, and interdisciplinary projects.
Assessment: Use questions requiring extended writing (multi-paragraph essays with introduction, body, conclusion), data analysis and interpretation, evaluation of competing viewpoints, problem-solving with real-world Nigerian applications, and synthesis of multiple sources. Questions should match WAEC/SSCE standard in both rigour and format. Require evidence, reasoning, and clear structure.`;
  }

  // ---- DEFAULT (if class name doesn't match expected patterns) ----
  return `== CLASS LEVEL GUIDANCE — ${className} ==
Adapt your language complexity, vocabulary, sentence structure, and conceptual depth to match ${className} level appropriately. Use BRITISH ENGLISH. Use Nigerian examples, names, places, and contexts. Ensure the content is suitable for the age group, cognitive development, and curriculum expectations of ${className} students in the Nigerian educational system.`;
}
