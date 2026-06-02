import { serve } from "https://deno.land/std@0.177.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

interface SubQuestion {
  label: string;
  text: string;
  guide: string;
}

interface QuestionOutput {
  question_text: string;
  options: Record<string, string> | null;
  correct_answer: string;
  type: "mcq" | "essay";
  format: string;
  marks: number;
  sub_questions?: SubQuestion[];
}

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

    const body = await req.json();
    const {
      lesson_note_contents,
      type,
      question_count,
      subject_name,
      class_name,
      difficulty,
      week_start,
      week_end,
      term,
    } = body;

    const month = new Date().toISOString().slice(0, 7);

    const { data: usage } = await supabase
      .from("ai_usage")
      .select("questions_generated")
      .eq("teacher_id", user.id)
      .eq("month", month)
      .single();

    const { data: profile } = await supabase
      .from("profiles")
      .select("plan")
      .eq("id", user.id)
      .single();

    const planName =
      profile?.plan === "advanced" ? "Advanced"
      : profile?.plan === "premium" ? "Premium"
      : "Basic";

    const { data: planLimits } = await supabase
      .from("plans")
      .select("questions_limit")
      .eq("name", planName)
      .single();

    const currentUsage = usage?.questions_generated || 0;
    const limit = planLimits?.questions_limit;
    if (limit !== null && limit !== undefined && currentUsage + question_count > limit) {
      return new Response(
        JSON.stringify({ error: "QUOTA_EXCEEDED", limit, current_usage: currentUsage }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 403 }
      );
    }

    const openrouterKey = Deno.env.get("OPENROUTER_API_KEY");
    if (!openrouterKey) throw new Error("AI service configuration missing (OPENROUTER_API_KEY)");

    const openrouterModel = Deno.env.get("OPENROUTER_MODEL") || "openai/gpt-4o-mini";

    const classLevelGuidance = getClassLevelGuidance(class_name);

    let difficultyInstruction: string;
    if (difficulty === "30-30-40") {
      const easyCount = Math.ceil(question_count * 0.3);
      const mediumCount = Math.ceil(question_count * 0.3);
      const hardCount = question_count - easyCount - mediumCount;
      difficultyInstruction = `${easyCount} easy, ${mediumCount} medium, ${hardCount} hard.`;
    } else {
      difficultyInstruction = `All ${question_count} questions must be ${difficulty} difficulty.`;
    }

    const combinedContent = lesson_note_contents
      .map((c: string, i: number) => `--- WEEK ${week_start + i} ---\n${c}`)
      .join("\n\n");

    let prompt: string;

    if (type === "mcq") {
      const directCount = Math.floor(question_count * 0.5);
      const remaining = question_count - directCount;
      const otherFormats = [
        "fill_gaps",
        "odd_one_out",
        "negative",
        "best_answer",
        "matching",
        "sequence",
        "situation",
        "multi_statement",
      ];

      let formatDist = `Format distribution: ${directCount} "direct" (straightforward question + 4 options). `;
      formatDist += `The remaining ${remaining} spread across: `;
      const assigned: string[] = [];
      for (let i = 0; i < remaining; i++) {
        assigned.push(`"${otherFormats[i % otherFormats.length]}"`);
      }
      formatDist += assigned.join(", ") + ". ";

      prompt = `You are a Nigerian school teacher creating multiple choice questions for an examination.

Subject: ${subject_name}
Class: ${class_name}
${term ? `Term: ${term} Term` : ""}
Week Range: Week ${week_start} - Week ${week_end}
Number of questions: ${question_count}
Difficulty: ${difficultyInstruction}
${formatDist}

== FORMAT DEFINITIONS ==
"direct" — straightforward question stem + 4 options A-D
"fill_gaps" — sentence with a blank; 4 options complete the blank
"odd_one_out" — "Which of the following is the odd one out?"; 4 options
"negative" — question using NOT, EXCEPT, or "which is false"; 4 options
"best_answer" — scenario; student picks the best/most appropriate among 4 options
"matching" — match Column A to Column B; 4 options A-D each describe a different mapping
"sequence" — arrange items in correct order; 4 options A-D each show a different sequence
"situation" — scenario provided, then a question; 4 options
"multi_statement" — statements I-II-III-IV given; student picks the correct combination; 4 options

ALL questions MUST have exactly 4 options labelled A, B, C, D with ONE clearly correct answer.

== CRITICAL INSTRUCTIONS ==
- Use BRITISH ENGLISH spelling, grammar, constructions (colour, centre, organise, behaviour, practise, licence, analyse, defence, metre, litre)
- ALL examples, names, places, foods, and contexts MUST be Nigerian: use names like Chinedu, Amina, Funke, Emeka, Ngozi; cities like Lagos, Abuja, Enugu, Kano, Port Harcourt, Ibadan; foods like jollof rice, egusi soup, suya, pounded yam, moi-moi, akara; institutions like NAFDAC, JAMB, NNPC, NITDA, NTA; sports like Super Eagles, Falconets; currency as naira and kobo
- Follow the CLASS LEVEL GUIDANCE below precisely — it determines vocabulary, sentence structure, conceptual depth, and question complexity

${classLevelGuidance}

== LESSON NOTES CONTENT ==
${combinedContent}

Return ONLY a valid JSON object — no markdown code fences, no surrounding text. Exact structure:
{
  "questions": [
    {
      "question_number": 1,
      "question_text": "...",
      "options": { "A": "...", "B": "...", "C": "...", "D": "..." },
      "correct_answer": "A",
      "format": "direct",
      "difficulty": "easy",
      "marks": 4,
      "explanation": "Why A is correct in one sentence"
    }
  ]
}`;
    } else {
      prompt = `You are a Nigerian school teacher creating theory (essay) examination questions.

Subject: ${subject_name}
Class: ${class_name}
${term ? `Term: ${term} Term` : ""}
Week Range: Week ${week_start} - Week ${week_end}
Number of questions: ${question_count}
Difficulty: ${difficultyInstruction}

Each theory question MUST have 2 or 3 sub-questions labelled (1a, 1b) or (1a, 1b, 1c).
Include an answer guide for EACH sub-question listing the key points a student should cover in their answer.

Questions should require critical thinking, analysis, explanation, and application — not simple recall.

== CRITICAL INSTRUCTIONS ==
- Use BRITISH ENGLISH spelling, grammar, constructions (colour, centre, organise, behaviour, practise, licence, analyse, defence, metre, litre)
- ALL examples, names, places, foods, and contexts MUST be Nigerian: use names like Chinedu, Amina, Funke, Emeka, Ngozi; cities like Lagos, Abuja, Enugu, Kano, Port Harcourt, Ibadan; foods like jollof rice, egusi soup, suya, pounded yam, moi-moi, akara; institutions like NAFDAC, JAMB, NNPC, NITDA, NTA; sports like Super Eagles, Falconets; currency as naira and kobo
- Follow the CLASS LEVEL GUIDANCE below precisely — it determines vocabulary, sentence structure, conceptual depth, and question complexity

${classLevelGuidance}

== LESSON NOTES CONTENT ==
${combinedContent}

Return ONLY a valid JSON object — no markdown code fences, no surrounding text. Exact structure:
{
  "questions": [
    {
      "question_number": 1,
      "question_text": "Main question or instruction (e.g. 'Discuss the importance of water purification in Nigerian communities.')",
      "sub_questions": [
        { "label": "1a", "text": "Define water purification.", "guide": "Key points: removal of contaminants, physical/chemical/biological processes, makes water safe for drinking and domestic use" },
        { "label": "1b", "text": "List and explain three methods of water purification commonly used in rural Nigerian communities.", "guide": "Key points: boiling — kills pathogens at 100C, simple and widely used; filtration — uses cloth/sand/charcoal to remove sediment; chlorination — water guards/tablets distributed by health centres" },
        { "label": "1c", "text": "Explain why waterborne diseases are still common in some parts of Nigeria despite knowledge of purification methods.", "guide": "Key points: poverty limits access to purification materials, cultural beliefs and practices, inadequate government water infrastructure, lack of education in remote areas" }
      ],
      "format": "essay",
      "difficulty": "medium",
      "marks": 15
    }
  ]
}`;
    }

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

    const responseText = data.choices?.[0]?.message?.content || "";

    let cleanedText = responseText.trim();
    cleanedText = cleanedText.replace(/^```(?:json)?\s*\n?/i, "").replace(/\n?```\s*$/, "");

    let parsed: { questions: any[] };
    try {
      parsed = JSON.parse(cleanedText);
    } catch {
      const jsonMatch = cleanedText.match(/\{[\s\S]*\}/);
      parsed = jsonMatch ? JSON.parse(jsonMatch[0]) : { questions: [] };
    }

    const aiQuestions = parsed.questions || [];

    let formattedText = "";
    const questionLabel = type === "mcq" ? "MCQ QUESTIONS" : "THEORY QUESTIONS";
    formattedText += `=== ${questionLabel} ===\n\n`;

    const termLabel = term ? (term.endsWith("Term") ? `Term: ${term}` : `Term: ${term} Term`) : "";
    formattedText += `Subject: ${subject_name}\n`;
    formattedText += `Class: ${class_name}\n`;
    if (termLabel) formattedText += `${termLabel}\n`;
    formattedText += `Week Range: Week ${week_start} - Week ${week_end}\n`;
    formattedText += `Total Questions: ${aiQuestions.length}\n`;
    formattedText += `Difficulty: ${difficulty}\n\n`;

    const questions: QuestionOutput[] = [];
    let sectionBAdded = false;
    const midpoint = Math.ceil(aiQuestions.length / 2) + 1;

    for (const q of aiQuestions) {
      const qNum = q.question_number;

      if (type === "mcq") {
        if (!sectionBAdded && qNum >= midpoint) {
          formattedText += `\n=== SECTION B ===\n\n`;
          sectionBAdded = true;
        }

        formattedText += `${qNum}. ${q.question_text}\n`;
        const opts = q.options || {};
        for (const key of ["A", "B", "C", "D"]) {
          if (opts[key] !== undefined) {
            formattedText += `   ${key}) ${opts[key]}\n`;
          }
        }
        formattedText += `\n`;

        questions.push({
          question_text: q.question_text,
          options: opts,
          correct_answer: q.correct_answer,
          type: "mcq",
          format: q.format || "direct",
          marks: q.marks || 4,
        });
      } else {
        formattedText += `${qNum}. ${q.question_text}\n`;
        if (Array.isArray(q.sub_questions)) {
          for (const sq of q.sub_questions) {
            formattedText += `   ${sq.label}) ${sq.text}\n`;
          }
        }
        formattedText += `\n`;

        const answerGuide = Array.isArray(q.sub_questions)
          ? q.sub_questions.map((sq: SubQuestion) => `${sq.label}) ${sq.guide}`).join("\n")
          : q.question_text;

        questions.push({
          question_text: q.question_text,
          options: null,
          correct_answer: answerGuide,
          type: "essay",
          format: "essay",
          marks: q.marks || 10,
          sub_questions: Array.isArray(q.sub_questions) ? q.sub_questions.map((sq: SubQuestion) => ({
            label: sq.label,
            text: sq.text,
            guide: sq.guide,
          })) : undefined,
        });
      }
    }

    formattedText += `\n=== ANSWER KEYS ===\n\n`;
    for (const q of aiQuestions) {
      const qNum = q.question_number;
      if (type === "mcq") {
        formattedText += `${qNum}. ${q.correct_answer} — ${q.explanation || "Correct answer"}\n`;
      } else {
        formattedText += `${qNum}. Answer Guide:\n`;
        if (Array.isArray(q.sub_questions)) {
          for (const sq of q.sub_questions) {
            formattedText += `   ${sq.label}) ${sq.guide}\n`;
          }
        }
        formattedText += `\n`;
      }
    }

    const newUsage = currentUsage + aiQuestions.length;
    await supabase
      .from("ai_usage")
      .upsert({
        teacher_id: user.id,
        month,
        questions_generated: newUsage,
        updated_at: new Date().toISOString(),
      }, { onConflict: "teacher_id,month" });

    const totalTokens = (data.usage?.prompt_tokens || 0) + (data.usage?.completion_tokens || 0);

    await supabase.from("ai_usage_logs").insert({
      teacher_id: user.id,
      feature: type === "mcq" ? "mcq" : "essay",
      tokens_used: totalTokens,
    });

    return new Response(
      JSON.stringify({
        formatted_text: formattedText,
        questions,
        total_questions: aiQuestions.length,
      }),
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
QUESTION COMPLEXITY: All questions must test RECALL and BASIC COMPREHENSION only. Use simple, one-step questions. MCQ options must be obviously distinct (no subtle distractors). Theory questions should ask students to name, list, identify, match, or state in ONE word or a short phrase (2-5 words). Do NOT expect full sentences in answers. No multi-step reasoning.
VOCABULARY: Use only the simplest, most concrete words a 6-9 year old Nigerian child knows. Avoid all abstract nouns, technical jargon, and academic register. If a new word is essential, embed it in a simple sentence with context clues.
LANGUAGE: Short, simple sentences (8-12 words). Present tense. Single idea per sentence. No complex clauses.
EXAMPLES: Nigerian children's daily world — home, family, school, local market, village animals, common foods, traditional games, familiar holidays. Characters: Ada, Chidi, Bisi, Emeka, Nkechi, Tunde.`;
    } else {
      return `== CLASS LEVEL GUIDANCE — PRIMARY ${level} (UPPER PRIMARY) ==
QUESTION COMPLEXITY: Questions should test RECALL, BASIC COMPREHENSION, and SIMPLE APPLICATION. MCQ options can include mild distractors but the correct answer should still be clearly defensible. Theory questions may ask students to explain in 2-3 sentences, describe in simple terms, compare two items, or give examples. Use "what", "where", "when", "how many", "list", "name", "describe", "give an example of".
VOCABULARY: Concrete vocabulary with occasional simple subject-specific terms introduced with definitions. Avoid abstract reasoning chains. Use words a 9-12 year old Nigerian child encounters in daily life and school.
LANGUAGE: Sentences of 10-15 words. Mix simple and compound sentences. Present tense primarily. Concrete, observable, factual.
EXAMPLES: Nigerian child's expanding world — neighbourhood, local town/city, Nigerian festivals, holidays, basic occupations, familiar Nigerian stories and fables. Characters with Nigerian names in realistic scenarios.`;
    }
  }

  // ---- JUNIOR SECONDARY (JSS 1-3) ----
  if (lower.startsWith("jss") || lower.includes("junior secondary")) {
    const level = parseInt(className.replace(/\D/g, "")) || 0;
    return `== CLASS LEVEL GUIDANCE — JSS ${level} (JUNIOR SECONDARY) ==
QUESTION COMPLEXITY: Questions should test COMPREHENSION, APPLICATION, and BASIC ANALYSIS. MCQ options should include plausible distractors — students must discriminate between close alternatives. Theory questions should require explanations in short paragraphs (5-8 sentences), comparisons, classifications, cause-effect explanations, and simple problem-solving. Use "explain", "compare", "contrast", "classify", "distinguish", "describe in detail", "what is the relationship between", "give reasons for".
VOCABULARY: Moderate vocabulary with subject-specific terminology used freely once defined. Students are building academic vocabulary. Use standard English appropriate for junior secondary level.
LANGUAGE: Sentences of 12-20 words. Vary sentence structure. Use appropriate academic register without being overly formal. Multi-step instructions acceptable.
EXAMPLES: Nigerian national life — cities, institutions (NAFDAC, JAMB, NNPC), industries, current events, Nigerian popular culture (Nollywood, Afrobeats, football), Nigerian professional contexts. Scenarios students may encounter or observe in their communities.`;
  }

  // ---- SENIOR SECONDARY (SSS 1-3) ----
  if (lower.startsWith("sss") || lower.startsWith("s.s.s") || lower.includes("senior secondary")) {
    const level = parseInt(className.replace(/\D/g, "")) || 0;
    return `== CLASS LEVEL GUIDANCE — SSS ${level} (SENIOR SECONDARY) ==
QUESTION COMPLEXITY: Questions should test ANALYSIS, SYNTHESIS, EVALUATION, and APPLICATION at WAEC/SSCE standard. MCQ options must include plausible distractors that test genuine understanding vs. memorisation. Theory questions must require extended writing (multi-paragraph essays with introduction, body, conclusion), critical evaluation of competing viewpoints, data analysis, problem-solving, and synthesis of multiple concepts. Use "evaluate", "critically analyse", "discuss", "justify", "to what extent", "suggest reasons why", "propose", "design", "assess the impact of".
VOCABULARY: Full academic vocabulary with subject-specific terminology required. Students should demonstrate command of technical language appropriate to the subject and WAEC/SSCE standard.
LANGUAGE: Full range of sentence structures. Academic register appropriate for students preparing for tertiary education. Sophisticated transitions and cohesive devices expected.
EXAMPLES: Nigerian and global contexts — economic policy, public health, environmental issues, technological innovation, political developments, legal frameworks, research and scholarship. Case studies from Nigerian businesses, government agencies, and current affairs. Connect to WAEC/SSCE examination expectations and real-world Nigerian applications.`;
  }

  // ---- DEFAULT ----
  return `== CLASS LEVEL GUIDANCE — ${className} ==
Adapt question complexity, vocabulary, sentence structure, and conceptual depth to match ${className} level in the Nigerian educational system. Use BRITISH ENGLISH. Use exclusively Nigerian examples, names, places, and contexts.`;
}
