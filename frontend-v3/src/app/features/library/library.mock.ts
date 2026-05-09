export interface MockLibraryItem {
  readonly id: string;
  readonly title: string;
  readonly podcast: string;
  readonly coverUrl: string;
}

const seeds = [
  ['the-floodcast', 'Le Floodcast', 'Florence Mendez & Adrien Ménielle'],
  ['nota-bene', 'Nota Bene', 'Benjamin Brillaud'],
  ['transfert', 'Transfert', 'Slate.fr'],
  ['code-source', 'Code source', 'Le Parisien'],
  ['affaires-sensibles', 'Affaires sensibles', 'France Inter'],
  ['les-pieds-sur-terre', 'Les pieds sur terre', 'France Culture'],
  ['choses-a-savoir', 'Choses à Savoir', 'Choses à Savoir'],
  ['la-poudre', 'La Poudre', "Lauren Bastide"],
  ['le-cours-de-l-histoire', "Le Cours de l'histoire", 'France Culture'],
  ['mecano', 'Mécano', 'Slate.fr'],
  ['les-baladeurs', 'Les Baladeurs', 'Paradiso Media'],
  ['programme-b', 'Programme B', 'Binge Audio'],
  ['superfail', 'Superfail', 'France Culture'],
  ['by-the-book', 'By the Book', 'France Culture'],
  ['vivons-heureux', 'Vivons heureux avant la fin du monde', 'ARTE Radio'],
  ['les-couilles-sur-la-table', 'Les Couilles sur la table', 'Binge Audio'],
];

export const MOCK_LIBRARY_ITEMS: readonly MockLibraryItem[] = seeds.map(
  ([slug, title, podcast]) => ({
    id: slug,
    title,
    podcast,
    coverUrl: `https://picsum.photos/seed/${slug}/400/400`,
  }),
);
