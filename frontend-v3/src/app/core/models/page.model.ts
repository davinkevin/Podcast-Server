export interface PageHAL<T> {
  readonly content: readonly T[];
  readonly empty: boolean;
  readonly first: boolean;
  readonly last: boolean;
  readonly number: number;
  readonly numberOfElements: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}
