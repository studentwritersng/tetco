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

    const body = await req.json();
    const {
      lesson_note_id,
      topic_title,
      subject_name,
      class_name,
      term,
      week_number,
      lesson_note_content,
    } = body;

    if (!topic_title || !subject_name || !class_name || !lesson_note_content) {
      return new Response(
        JSON.stringify({ error: "Missing required fields: topic_title, subject_name, class_name, lesson_note_content" }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 400 }
      );
    }

    const month = new Date().toISOString().slice(0, 7);

    const { data: usage } = await supabase
      .from("ai_usage")
      .select("teaching_guides_generated")
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
      .select("teaching_guide_limit")
      .eq("name", planName)
      .single();

    const currentUsage = usage?.teaching_guides_generated || 0;
    const limit = planLimits?.teaching_guide_limit;
    if (limit !== null && limit !== undefined && currentUsage >= limit) {
      return new Response(
        JSON.stringify({ error: "QUOTA_EXCEEDED", limit }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 403 }
      );
    }

    const openrouterKey = Deno.env.get("OPENROUTER_API_KEY");
    if (!openrouterKey) throw new Error("AI service configuration missing (OPENROUTER_API_KEY)");

    const openrouterModel = Deno.env.get("OPENROUTER_MODEL") || "openai/gpt-4o-mini";

    const subjectGuidance = getSubjectGuidance(subject_name);
    const classLevelGuidance = getClassLevelGuidance(class_name);

    const prompt = `You are a highly experienced Nigerian master teacher and pedagogy specialist, well-versed in the Nigerian educational curriculum and classroom realities. You are preparing a detailed teaching guide for a fellow teacher.

PARAMETERS:
Subject: ${subject_name}
Class: ${class_name}
Topic: ${topic_title}
Term: ${term} Term
Week: ${week_number}

${subjectGuidance}

${classLevelGuidance}

LESSON NOTE CONTENT:
${lesson_note_content}

== YOUR TASK ==

Write a comprehensive, practical teaching guide for the topic above. Structure your response under EXACTLY the following ten headings. Use the heading text precisely as shown — the teacher's interface will parse these headings.

HISTORICAL AND BACKGROUND CONTEXT
Provide 2-3 paragraphs on the historical development, origin, or real-world significance of this topic. Explain why it matters in the wider world and specifically within the Nigerian context. Mention key figures, discoveries, events, or milestones where relevant. Where possible, connect to Nigeria's own history, industries, or development. Use British English spelling and grammar throughout.

KEY CONCEPTS AND POINTS TO EMPHASISE
List 4-6 essential concepts or principles the teacher MUST ensure students understand by the end of the lesson. For each point, state it as a clear learning takeaway and add a sentence on how to reinforce it. BOLD the key terms or phrases within each point so they stand out visually when the teacher reads the guide. Example format: "Students must understand that **photosynthesis** is the process by which plants convert **sunlight into chemical energy**. Reinforce this by having students explain it back in their own words."

COMMON MISCONCEPTIONS
Identify 3-4 misconceptions students commonly hold about this topic. For each one, state the misconception clearly, explain why it is wrong, and provide a practical strategy the teacher can use to pre-emptively address it before it takes root in students' minds.

REAL-LIFE NIGERIAN EXAMPLES
Provide 3-5 concrete, vivid examples drawn from Nigerian life, culture, and environment that the teacher can use to make the topic relatable. Draw from Lagos markets, Nigerian industries, local customs, Nigerian foods, Nollywood, Nigerian sports, well-known Nigerian landmarks, businesses, or everyday scenes. Describe each example in enough detail that a teacher can confidently present it to the class without additional research.

PEDAGOGICAL STRATEGY
Recommend the most effective teaching method(s) for this specific subject and topic — e.g. inquiry-based learning, demonstration, discussion, guided discovery, cooperative learning, project-based approach, storytelling, role play, Socratic questioning. Explain why this approach is suited to both the subject matter and the Nigerian classroom context. Suggest how to manage the typical Nigerian class size (often 30-60+ students) with this method.

LESSON FLOW AND TIMING
Provide a minute-by-minute breakdown of how to structure a single lesson period (typically 40 minutes for Nigerian secondary schools, though adjust if the subject context suggests otherwise). Divide it into clear phases: Starter/Entry activity, Main teaching input, Guided practice/group work, Independent practice, and Plenary/Wrap-up. Give the teacher clear instructions for what to do in each phase. Even if the lesson note specifies a different duration, use 40 minutes as the standard.

DIFFERENTIATION
Provide specific, actionable strategies for supporting weaker students and challenging stronger ones within this particular topic. For weaker students: suggest simplified explanations, scaffolding techniques, additional visual aids, or peer support approaches. For stronger students: suggest extension tasks, higher-order questions, independent research prompts, or leadership roles during group work. Make suggestions that work in a typical Nigerian classroom without specialised resources.

DISCUSSION QUESTIONS
Provide 5-6 open-ended, thought-provoking questions the teacher can pose to spark whole-class or small-group discussion. Questions should encourage critical thinking, connect the topic to students' own experiences in Nigeria, and promote active engagement. Avoid simple yes/no questions. For each question, add a brief note in parentheses on what kind of responses to expect or how to guide the discussion.

ASSESSMENT CHECKPOINTS
Describe 3-4 moments during the lesson where the teacher should pause to check whether students are understanding the material. For each checkpoint, specify when in the lesson flow it occurs, what specific technique to use (e.g. targeted questioning, mini-whiteboard response, thumbs up/down, quick pair-share, exit ticket), and what the teacher should do if students are not grasping the concept.

CROSS-CURRICULAR LINKS
Identify 2-3 meaningful connections between this topic and other subjects in the Nigerian curriculum. Explain how the topic intersects with each linked subject, and suggest a specific idea the teacher can mention to help students see the interconnectedness of knowledge. For example, a mathematics topic might link to economics (market pricing) or geography (map coordinates).

== STYLE INSTRUCTIONS ==
- Use BRITISH ENGLISH spelling, grammar, and constructions exclusively (colour, centre, organise, behaviour, analyse, defence, metre, litre, programme, practise as a verb, licence as a noun)
- ALL examples, names, places, foods, and cultural references MUST be Nigerian — use cities like Lagos, Abuja, Kano, Enugu, Port Harcourt, Ibadan; foods like jollof rice, egusi, suya, pounded yam, moi-moi; institutions like NAFDAC, JAMB, NNPC, NITDA, NAFDAC, FRSC; currency as naira and kobo; names like Chinedu, Amina, Funke, Emeka, Ngozi, Bisi
- Be practical, direct, and classroom-ready — every suggestion should be something a teacher can implement tomorrow, even in a resource-constrained classroom
- Write in warm, supportive, professional prose as if guiding a colleague
- Do NOT use markdown code fences or wrap the output in any formatting other than plain text with the ten headings above
- Do NOT include any introductory or concluding remarks beyond the ten sections — start directly with the first heading

Now write the complete teaching guide.`;

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

    const teaching_guide = data.choices?.[0]?.message?.content || "";

    if (!teaching_guide) {
      return new Response(
        JSON.stringify({ error: "Failed to generate teaching guide" }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 500 }
      );
    }

    if (lesson_note_id) {
      const { error: updateError } = await supabase
        .from("lesson_notes")
        .update({ teaching_guide, updated_at: new Date().toISOString() })
        .eq("id", lesson_note_id);

      if (updateError) {
        console.error("Failed to save teaching guide to lesson_note:", updateError.message);
      }
    }

    await supabase
      .from("ai_usage")
      .upsert({
        teacher_id: user.id,
        month,
        teaching_guides_generated: currentUsage + 1,
        updated_at: new Date().toISOString(),
      }, { onConflict: "teacher_id,month" });

    const totalTokens = (data.usage?.prompt_tokens || 0) + (data.usage?.completion_tokens || 0);

    await supabase.from("ai_usage_logs").insert({
      teacher_id: user.id,
      feature: "teaching_guide",
      tokens_used: totalTokens,
    });

    return new Response(
      JSON.stringify({ teaching_guide }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (error) {
    return new Response(
      JSON.stringify({ error: error.message }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 500 }
    );
  }
});

function getSubjectGuidance(subject: string): string {
  const lower = subject.toLowerCase();

  if (lower.includes("mathematics") || lower.includes("maths") || lower.includes("further")) {
    return `SUBJECT-SPECIFIC GUIDANCE — MATHEMATICS:
- Prioritise procedural fluency alongside conceptual understanding — students need both the how and the why
- Emphasise step-by-step worked examples before independent practice
- Common challenge: students memorise formulas without understanding when to apply them
- Use manipulatives and visual representations where possible (e.g. number lines, diagrams, geometric shapes)
- Connect abstract concepts to practical Nigerian contexts: market transactions, building measurements, agricultural calculations, budgeting`;
  }

  if (lower.includes("english") || lower.includes("literature")) {
    return `SUBJECT-SPECIFIC GUIDANCE — ENGLISH / LITERATURE:
- Balance explicit skill instruction (grammar, structure, technique) with creative and critical engagement
- Model the skill or response you expect before asking students to produce their own
- Use Nigerian literature, Nollywood films, Afrobeats lyrics, and local storytelling traditions as entry points
- For writing tasks: guide students through planning, drafting, peer review, and revision as distinct stages
- Encourage code-switching awareness — help students distinguish between informal Nigerian English and formal register`;
  }

  if (lower.includes("science") || lower.includes("biology") || lower.includes("chemistry") || lower.includes("physics")) {
    return `SUBJECT-SPECIFIC GUIDANCE — SCIENCE:
- Prioritise hands-on demonstration; if laboratory equipment is unavailable, use everyday Nigerian household items (e.g. plastic bottles for measuring cylinders, salt and water for solutions, local fruits for pH testing)
- Emphasise the scientific method: observation, hypothesis, experimentation, conclusion
- Connect every topic to a real Nigerian phenomenon — health, agriculture, environment, industry, cooking
- Students often struggle to visualise abstract scientific processes; use diagrams, analogies, and physical models
- Highlight Nigerian scientists and their contributions where relevant (e.g. Philip Emeagwali, Grace Alele-Williams)`;
  }

  if (lower.includes("social studies") || lower.includes("civic") || lower.includes("government") || lower.includes("history")) {
    return `SUBJECT-SPECIFIC GUIDANCE — SOCIAL STUDIES / CIVIC EDUCATION:
- Ground abstract concepts (democracy, rights, governance) in students' lived experience in Nigerian communities
- Encourage debate, role-play, and simulations — students learn citizenship by practising it
- Use current Nigerian events and newspapers as teaching resources
- Beware of students conflating what ought to be (ideals) with what is (Nigerian realities) — guide critical but constructive analysis
- Connect historical events to present-day Nigerian institutions and political structures`;
  }

  if (lower.includes("economics") || lower.includes("commerce") || lower.includes("business") || lower.includes("accounting") || lower.includes("financial")) {
    return `SUBJECT-SPECIFIC GUIDANCE — ECONOMICS / COMMERCE:
- Anchor every concept in a familiar Nigerian market or business scenario (e.g. Balogun Market, Alaba International, a local provision shop)
- Use real Nigerian price data, exchange rates, and economic indicators where relevant
- Encourage students to interview local business owners or observe market transactions as fieldwork
- Common challenge: students memorise definitions without applying them to real economic situations — use case studies and scenario-based questions
- Link to current Nigerian economic policies, budget announcements, and business news`;
  }

  if (lower.includes("agricultural") || lower.includes("agric")) {
    return `SUBJECT-SPECIFIC GUIDANCE — AGRICULTURAL SCIENCE:
- Nigeria is an agrarian economy — ground every lesson in the reality that most students' families engage in or interact with farming
- Use Nigerian crops (cassava, yam, maize, rice, cocoa, groundnut, oil palm), livestock, and farming systems as primary examples
- Discuss practical challenges Nigerian farmers face: access to credit, fertiliser, storage, market access, climate variability
- If a school farm exists, plan the lesson to incorporate observation or practical work on the farm
- Connect to Nigerian agricultural policies, research institutes (IITA, NCRI), and agribusiness opportunities`;
  }

  if (lower.includes("religious") || lower.includes("crs") || lower.includes("irk") || lower.includes("christian") || lower.includes("islamic")) {
    return `SUBJECT-SPECIFIC GUIDANCE — RELIGIOUS STUDIES:
- Approach the topic from an academic, comparative perspective that respects Nigeria's religious diversity
- Use scriptural references and stories as teaching texts, but encourage analytical discussion rather than purely devotional reading
- Connect moral and ethical principles to contemporary Nigerian social issues
- Be sensitive to students of different faith backgrounds in the classroom — frame discussion inclusively
- Encourage students to identify shared values across religious traditions`;
  }

  if (lower.includes("computer") || lower.includes("ict") || lower.includes("data") || lower.includes("information")) {
    return `SUBJECT-SPECIFIC GUIDANCE — COMPUTER STUDIES / ICT:
- Acknowledge the reality that many Nigerian schools have limited computer access — plan both theory and practical alternatives
- Use mobile phones as a relatable reference point since most students have exposure to them
- Where physical computers are unavailable, use diagrams, printouts, and role-play simulations (e.g. students acting out how a CPU processes instructions)
- Highlight Nigerian tech success stories (Andela, Flutterwave, Paystack, Interswitch) to inspire students
- Emphasise digital literacy and online safety, which are critical for Nigerian youth`;
  }

  if (lower.includes("geography")) {
    return `SUBJECT-SPECIFIC GUIDANCE — GEOGRAPHY:
- Use Nigeria's diverse physical geography as the primary laboratory: from the Sahel in the north to the Niger Delta in the south
- Reference specific Nigerian landforms, climate patterns, vegetation zones, and population distributions
- Connect physical geography to human geography: how Nigeria's environment shapes settlement, agriculture, and economic activity
- If maps are scarce, have students draw sketch maps of their local area or state
- Discuss contemporary Nigerian environmental issues: desertification, erosion in the southeast, flooding in coastal cities`;
  }

  if (lower.includes("yoruba") || lower.includes("igbo") || lower.includes("hausa")) {
    return `SUBJECT-SPECIFIC GUIDANCE — NIGERIAN LANGUAGE:
- Create an immersive environment — use the target language for classroom instructions, greetings, and routines
- Draw on proverbs, folktales, songs, and oral traditions as rich teaching resources
- Encourage students to practise speaking in pairs and small groups, not just reading and writing
- Connect language learning to cultural practices: festivals, naming ceremonies, traditional occupations, food
- Be patient with urban students who may have limited exposure to the language at home — build confidence before accuracy`;
  }

  return `SUBJECT-SPECIFIC GUIDANCE:
- Ground the topic in the Nigerian educational context and curriculum expectations for ${subject} at ${class_name} level
- Use teaching methods appropriate to this subject discipline
- Connect theoretical content to practical, observable phenomena in the students' Nigerian environment
- Anticipate the typical challenges Nigerian students face with this subject area`;
}

function getClassLevelGuidance(className: string): string {
  const lower = className.toLowerCase();

  // ---- PRIMARY (1-6) ----
  if (lower.startsWith("primary")) {
    const level = parseInt(className.replace(/\D/g, "")) || 0;
    if (level <= 3) {
      return `== CLASS LEVEL GUIDANCE — PRIMARY ${level} (LOWER PRIMARY) ==
PEDAGOGICAL APPROACH: Use storytelling, songs, rhymes, repetition, role-play, and hands-on activities. Every lesson should feel like guided play with clear learning goals. Students learn by doing, touching, seeing, and moving — minimise passive listening. Use lots of praise and encouragement. Maintain a slow, patient pace with frequent repetition.
LANGUAGE & EXPLANATIONS: Use the simplest possible vocabulary — the kind a 6-9 year old Nigerian child uses at home and on the playground. One idea per sentence. Give single-step instructions. Avoid all abstract terms, technical jargon, and academic language. Explain new concepts using concrete objects the child can see or touch.
NIGERIAN EXAMPLES: Draw exclusively from the immediate environment — home, family, school compound, the local market, familiar animals (goat, chicken, cow, dog), common Nigerian foods (rice, yam, beans, bread, plantain, mango), traditional children's games (ampe, ten-ten, hide-and-seek, football), and familiar places (the village, the church/mosque, the shop). Use Nigerian children's names for characters.
DIFFERENTIATION: For weaker students — pair with stronger peers, use more pictures and physical objects, accept oral responses instead of written, simplify tasks further. For stronger students — ask them to help peers, give them slightly harder examples within the same concrete framework, ask "what if" questions about familiar scenarios.
DISCUSSION QUESTIONS: Must be concrete, personal, and experience-based: "What did you eat this morning?", "How do you go to school?", "Who helps you with homework at home?". The goal is oral participation, not analytical depth.
ASSESSMENT: Use observation, oral questioning, show-and-tell, drawing and labelling, matching exercises, and simple yes/no or thumbs-up/thumbs-down checks. Never use written tests for formative assessment at this level.`;
    } else {
      return `== CLASS LEVEL GUIDANCE — PRIMARY ${level} (UPPER PRIMARY) ==
PEDAGOGICAL APPROACH: Use guided discovery, group work, simple experiments or demonstrations, short writing tasks, and structured discussion. Students can handle simple investigations with guidance. Maintain a lively, participatory classroom. Introduce basic note-taking and simple report writing. Use both individual and group activities.
LANGUAGE & EXPLANATIONS: Use concrete vocabulary with occasional subject-specific terms defined clearly. Sentences of moderate length. Give 2-3 step instructions. Present concepts as facts with simple explanations — connect new ideas explicitly to what students already know. Use "you already know that..." bridging statements.
NIGERIAN EXAMPLES: Draw from the child's expanding Nigerian world — the neighbourhood, local government area, familiar cities (Lagos, Ibadan, Kano, Enugu), Nigerian holidays and festivals, basic occupations (farmer, trader, teacher, nurse, driver, tailor), Nigerian food crops and markets, familiar Nigerian stories and fables. Introduce simple Nigerian civic concepts.
DIFFERENTIATION: For weaker students — provide sentence starters, word banks, visual aids, shorter writing tasks, extra teacher attention during group work. For stronger students — open-ended questions, "investigate further" prompts, leadership roles in group activities, additional challenge questions.
DISCUSSION QUESTIONS: Connect to students' observations and experiences: "Have you ever seen...?", "Why do you think...?", "What would happen if...?". Encourage students to give reasons and provide examples from their own lives.
ASSESSMENT: Short-answer questions, fill-in-the-blank, matching, simple true/false with correction, listing, labelling diagrams, 3-5 sentence written responses. Questions test recall and basic comprehension.`;
    }
  }

  // ---- JUNIOR SECONDARY (JSS 1-3) ----
  if (lower.startsWith("jss") || lower.includes("junior secondary")) {
    const level = parseInt(className.replace(/\D/g, "")) || 0;
    return `== CLASS LEVEL GUIDANCE — JSS ${level} (JUNIOR SECONDARY) ==
PEDAGOGICAL APPROACH: Use a mix of teacher-guided and student-centred methods — group discussions, debates, guided discovery, simple research projects, practical work, case studies, and role-play. Students should increasingly take responsibility for their own learning. Introduce academic discipline-specific thinking habits. Prepare students for the transition to senior secondary rigour.
LANGUAGE & EXPLANATIONS: Use a moderate vocabulary with subject-specific terminology. Define terms clearly when introduced. Use varied sentence structures. Give multi-step instructions. Present concepts with both definitions and explanations of real-world significance. Encourage students to rephrase ideas in their own words.
NIGERIAN EXAMPLES: Draw from Nigerian national life — cities, institutions (NAFDAC, JAMB, NNPC, NITDA, NTA, FRSC), industries (banking, telecoms, agriculture, entertainment, technology), current events, and popular culture. Use Nigerian professional contexts and realistic scenarios that students may encounter in their communities.
DIFFERENTIATION: For weaker students — provide structured worksheets, guided notes, simplified reading materials, extra scaffolding during complex tasks, peer tutoring. For stronger students — extension tasks, independent research, higher-order questions requiring analysis and evaluation, opportunities to present to the class.
DISCUSSION QUESTIONS: Require explanation, comparison, and justification: "Compare...", "What are the advantages and disadvantages of...?", "Why is this important for Nigeria?", "What might happen if...?". Encourage students to support opinions with evidence and examples from Nigerian life.
ASSESSMENT: Short paragraphs (5-8 sentences), explain-in-your-own-words, compare-and-contrast, cause-and-effect, basic data interpretation (tables, charts, graphs), application of concepts to new Nigerian scenarios. Questions should require comprehension and basic analysis.`;
  }

  // ---- SENIOR SECONDARY (SSS 1-3) ----
  if (lower.startsWith("sss") || lower.startsWith("s.s.s") || lower.includes("senior secondary")) {
    const level = parseInt(className.replace(/\D/g, "")) || 0;
    return `== CLASS LEVEL GUIDANCE — SSS ${level} (SENIOR SECONDARY) ==
PEDAGOGICAL APPROACH: Use seminar-style teaching, Socratic questioning, problem-based learning, extended investigations, laboratory work (where applicable), independent research, and critical analysis of primary and secondary sources. Students should develop independent, critical thinking appropriate for WAEC/SSCE, JAMB/UTME, and tertiary education. Foster academic debate and evidence-based argumentation.
LANGUAGE & EXPLANATIONS: Full academic register with subject-specific terminology used precisely. Expect students to engage with complex texts and articulate sophisticated ideas. Use advanced vocabulary, complex sentence structures, and academic conventions. Connect topics to scholarly debates and unresolved questions in the field.
NIGERIAN EXAMPLES: Draw from Nigerian and global contexts — economic policy, public health challenges, environmental issues, technological innovation, political developments, legal frameworks, research and scholarship. Reference WAEC/SSCE past questions and examination expectations. Use case studies from Nigerian businesses, government agencies, research institutions, and current affairs at local, national, and international levels.
DIFFERENTIATION: For weaker students — provide additional reading support, structured essay frameworks, vocabulary banks, extra tutorial guidance, model answers. For stronger students — challenge with open-ended investigations, advanced reading materials, leadership in group work, peer teaching opportunities, preparation for academic competitions and Olympiads.
DISCUSSION QUESTIONS: Require critical evaluation, synthesis, and justification: "Critically evaluate...", "To what extent do you agree...?", "Analyse the impact of... on Nigerian society", "Propose solutions to...", "Assess the effectiveness of...". Expect students to engage with multiple perspectives and construct evidence-based arguments referencing Nigerian and global examples.
ASSESSMENT: Extended multi-paragraph essays with clear argumentation, data analysis and interpretation, evaluation of competing viewpoints, problem-solving with real-world Nigerian applications, synthesis of multiple concepts and sources. Assessment should match WAEC/SSCE standard in both rigour and format. Require evidence, reasoning, and structured presentation.`;
  }

  // ---- DEFAULT ----
  return `== CLASS LEVEL GUIDANCE — ${className} ==
Adapt pedagogical approach, language complexity, examples, and assessment strategies to match ${className} level in the Nigerian educational system. Use BRITISH ENGLISH. Use exclusively Nigerian examples, names, places, and contexts. Ensure all recommendations are practical and implementable in a resource-constrained Nigerian classroom.`;
}
