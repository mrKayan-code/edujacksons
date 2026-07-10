package dev.edujacksons.judge.api;

/**
 * Имена Kafka-топиков конвейера проверки. Владелец контракта — модуль judge (кандидат №1
 * на вынос в микросервис). {@code REQUESTS} — заявки от submissions, {@code RESULTS} — итоги.
 */
public final class JudgeTopics {

    public static final String REQUESTS = "judge.requests";
    public static final String RESULTS = "judge.results";

    private JudgeTopics() {
    }
}
