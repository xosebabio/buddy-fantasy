import type { components } from '../../core/api/schema';

export type Position = components['schemas']['PlayerResponse']['position'];

/** A position in text, next to the bib or in its chip (FE-23: a new position breaks the build). */
export const POSITION_TEXTS: Record<Position, string> = {
  GOALKEEPER: $localize`:@@shared.position.goalkeeper:Portero`,
  DEFENDER: $localize`:@@shared.position.defender:Defensa`,
  MIDFIELDER: $localize`:@@shared.position.midfielder:Medio`,
  FORWARD: $localize`:@@shared.position.forward:Delantero`,
};

/** The title of a group of players of a position, such as the candidates of a lineup. */
export const POSITION_GROUP_TEXTS: Record<Position, string> = {
  GOALKEEPER: $localize`:@@shared.position.goalkeepers:Porteros`,
  DEFENDER: $localize`:@@shared.position.defenders:Defensas`,
  MIDFIELDER: $localize`:@@shared.position.midfielders:Medios`,
  FORWARD: $localize`:@@shared.position.forwards:Delanteros`,
};

/** The positions from the own goal forwards: the order of lists and of the rows of the pitch. */
export const POSITIONS: readonly Position[] = ['GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD'];

/** The class that paints something in the color of a position (`--bf-color-position-*`). */
export const POSITION_CLASSES: Record<Position, string> = {
  GOALKEEPER: 'goalkeeper',
  DEFENDER: 'defender',
  MIDFIELDER: 'midfielder',
  FORWARD: 'forward',
};
