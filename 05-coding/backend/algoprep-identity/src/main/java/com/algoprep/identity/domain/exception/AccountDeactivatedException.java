package com.algoprep.identity.domain.exception;

import com.algoprep.common.exception.ClientErrorDetails;
import com.algoprep.common.exception.PermissionDeniedException;
import com.algoprep.identity.domain.model.RestorationTicket;
import java.util.Map;

/** The message is safe for logging; the optional ticket belongs only in the response details. */
public class AccountDeactivatedException extends PermissionDeniedException
    implements ClientErrorDetails {
  private static final long serialVersionUID = 1L;
  private final RestorationTicket ticket;

  public AccountDeactivatedException(RestorationTicket ticket) {
    super(IdentityErrorCodes.ACCOUNT_DEACTIVATED, "Account is deactivated");
    this.ticket = ticket;
  }

  @Override
  public Map<String, Object> clientErrorDetails() {
    return ticket == null
        ? Map.of()
        : Map.of("restoreTicket", ticket.value(), "expiresInSeconds", ticket.expiresInSeconds());
  }
}
