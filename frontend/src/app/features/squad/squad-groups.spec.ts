import { squadGroups } from './squad-groups';
import { PLAYERS } from '../../shared/roster/players.testing';

describe('squadGroups', () => {
  it('groups the active players by position from the own goal, each group by name', () => {
    const groups = squadGroups(PLAYERS);

    expect(groups.map((group) => group.position)).toEqual(['GOALKEEPER', 'FORWARD']);
    expect(groups[1]?.players.map((player) => player.name)).toEqual(['Alberto', 'Pablo']);
  });

  it('leaves out the retired players and the positions that end up empty', () => {
    expect(squadGroups(PLAYERS).flatMap((group) => group.players.map((p) => p.name))).not.toContain(
      'Xurxo',
    );
    expect(squadGroups([])).toEqual([]);
  });
});
