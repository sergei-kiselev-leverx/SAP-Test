package com.sap.codelab.view.home

/**
 * Defines when and why the location service should be started.
 */
sealed interface LocationServiceStartState {
    /** No active memos with location — service not needed. */
    data object NoReasonToStart : LocationServiceStartState

    /** Service cannot start until permissions are granted. */
    data object RequestPermissions : LocationServiceStartState

    /** Conditions met — service should start. */
    data object ShouldStart : LocationServiceStartState
}