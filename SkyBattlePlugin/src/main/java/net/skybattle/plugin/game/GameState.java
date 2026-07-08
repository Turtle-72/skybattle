package net.skybattle.plugin.game;

/**
 * Состояния игрового цикла Sky Battle.
 */
public enum GameState {

    /** Лобби: игроки ждут начала раунда. */
    WAITING,

    /** Отсчёт перед стартом. */
    STARTING,

    /** Активная фаза боя. */
    IN_GAME,

    /** Раунд завершён, показ результатов и подготовка к следующему. */
    ENDED
}
