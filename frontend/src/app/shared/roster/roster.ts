import type { MemberResponse } from '../../core/api/league/league-api';
import type { PlayerResponse } from '../../core/api/squad/squad-api';

const UNKNOWN_NAME = $localize`:@@shared.roster.unknownName:Usuario sin nombre`;
const FORMER_MEMBER = $localize`:@@shared.roster.formerMember:Antiguo miembro`;

/**
 * The squad by id, to cross the player ids of lineups, sheets, points and histories. Built from the
 * whole squad (`includeRetired`), so a retired player is still found.
 */
export function playerIndex(
  players: readonly PlayerResponse[],
): ReadonlyMap<string, PlayerResponse> {
  return new Map(players.map((player) => [player.id, player]));
}

/** The league's members by id, to cross the user ids of standings, lineups and histories. */
export function memberIndex(
  members: readonly MemberResponse[],
): ReadonlyMap<string, MemberResponse> {
  return new Map(members.map((member) => [member.userId, member]));
}

/** How a member is named, also when they have no name in their Google account. */
export function displayName(member: MemberResponse): string {
  return member.displayName ?? UNKNOWN_NAME;
}

/**
 * The name of a user of the league, or «Antiguo miembro» if they are no longer a member: they left
 * or were removed, but their lineups and points remain.
 */
export function memberName(index: ReadonlyMap<string, MemberResponse>, userId: string): string {
  const member = index.get(userId);
  return member === undefined ? FORMER_MEMBER : displayName(member);
}
