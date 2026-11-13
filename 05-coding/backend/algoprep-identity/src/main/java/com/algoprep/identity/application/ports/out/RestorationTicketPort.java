package com.algoprep.identity.application.ports.out;

import com.algoprep.identity.domain.model.RestorationGrant;
import java.util.Optional;

/** Ticket storage uses hashes only; no session/access-token authority is granted here. */
public interface RestorationTicketPort {
  void save(String ticketHash, RestorationGrant grant);

  Optional<RestorationGrant> find(String ticketHash);

  /** Called only after the restoration transaction commits. */
  void delete(String ticketHash);
}
