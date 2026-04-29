package com.markov.agent.data_loader.importer;

import com.markov.agent.data_loader.value_object.Company;

import java.util.List;

public record ImportResult(List<Company> companies, Integer lastCompanyId, ImportStatus status) {

    private static final ImportResult EMPTY = new ImportResult(List.of(), null, ImportStatus.EMPTY);

    public static ImportResult empty() {
        return EMPTY;
    }

    public static ImportResult error() {
        return new ImportResult(List.of(), null, ImportStatus.ERROR);
    }

    public boolean isEmpty() {
        return status == ImportStatus.EMPTY;
    }

    public boolean isError() {
        return status == ImportStatus.ERROR;
    }

    public boolean isSuccess() {
        return status == ImportStatus.SUCCESS;
    }

    public enum ImportStatus {
        SUCCESS,
        EMPTY,
        ERROR
    }
}