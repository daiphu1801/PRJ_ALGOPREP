/** `page` of a paged F2 answer (`03-dd/api/F2_problem-bank.md` 1.2): `number` is 1-based. */
export type PageMeta = {
  number: number;
  size: number;
  totalItems: number;
  totalPages: number;
};
