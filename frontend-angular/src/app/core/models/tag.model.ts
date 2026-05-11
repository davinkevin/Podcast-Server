export interface TagHAL {
  readonly id: string;
  readonly name: string;
}

export interface TagsContainerHAL {
  readonly content: readonly TagHAL[];
}

/** Tag input shape used in podcast create/update bodies. */
export interface TagInput {
  readonly id?: string;
  readonly name: string;
}
