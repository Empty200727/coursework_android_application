package ru.kinopolka.core.model;

/**
 * «Хочу посмотреть» and «Смотрел» are mutually exclusive, so they are one status field;
 * «Избранное» is an independent flag (see the status rule in docs/PLAN.md, section 2).
 */
public enum WatchStatus {
    NONE,
    WANT,
    WATCHED
}
