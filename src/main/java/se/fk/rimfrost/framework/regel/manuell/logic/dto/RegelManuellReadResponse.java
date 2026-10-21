package se.fk.rimfrost.framework.regel.manuell.logic.dto;

/**
 * Wraps the rule-specific response from {@code GET /{handlaggningId}} together with the data
 * a BFF needs to publish an informationsaccess event (FRMM-FR-09).
 *
 * @param data the rule-specific response returned by {@code readData()}
 * @param informationsaccess the informationsaccess data for the read
 * @param <T> the rule-specific response type
 */
public record RegelManuellReadResponse<T>(T data, Informationsaccess informationsaccess)
{
}
