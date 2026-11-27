import type { AuditEvent, AuditLogParams } from "./types";

/**
 * True when a row belongs to the list the filters describe (BR-SC-01). It mirrors what the server
 * does with the text (F1 #39: actor e-mail, event code as an id prefix with an optional `evt_`, or
 * the service name), so a row the live stream delivers is kept or dropped the way a reload would.
 * The mock list uses the same function, with the actor's name where it has no e-mail.
 */
export function matchesFilters(
  event: AuditEvent,
  params: AuditLogParams,
): boolean {
  const needle = params.query.trim().toLowerCase();
  const code = needle.startsWith("evt_") ? needle.slice(4) : needle;
  const textOk =
    !needle ||
    event.service.toLowerCase().includes(needle) ||
    (event.actorEmail ?? event.actor).toLowerCase().includes(needle) ||
    event.id.toLowerCase().replace(/^evt_/, "").startsWith(code);
  return (
    (params.category === "all" || event.category === params.category) &&
    (!params.from || event.date >= params.from) &&
    (!params.to || event.date <= params.to) &&
    textOk
  );
}
