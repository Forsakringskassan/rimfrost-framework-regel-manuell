package se.fk.rimfrost.framework.regel.manuell.logic.dto;

/**
 * The inloggad handläggare's identity in {@link Informationsaccess} (FRMM-FR-09.5).
 *
 * @param typId the id of the identifier type
 * @param varde the identifier value
 */
public record Anvandare(String typId,String varde){}
