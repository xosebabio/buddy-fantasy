import { DESTINATIONS, destinationOf } from './destinations';

describe('destinationOf', () => {
  it.each([
    ['', 'Jornada'],
    ['lineup', 'Alineación'],
    ['lineups', 'Alineación'],
    ['ratings', 'Valorar'],
    ['standings', 'Clasificación'],
    ['managers', 'Clasificación'],
    ['league', 'Liga'],
    ['squad', 'Liga'],
    ['matches', 'Liga'],
    ['rules', 'Liga'],
    ['members', 'Liga'],
    ['admin', 'Liga'],
  ])('puts the pages under «%s» in «%s»', (section, label) => {
    expect(destinationOf(section)?.label).toBe(label);
  });

  it('finds none for a page that no destination covers', () => {
    expect(destinationOf('unknown')).toBeUndefined();
  });

  it('covers each prefix in a single destination', () => {
    const prefixes = DESTINATIONS.flatMap((destination) => destination.prefixes);

    expect(new Set(prefixes).size).toBe(prefixes.length);
  });

  it('links each destination to a route it covers', () => {
    for (const destination of DESTINATIONS) {
      expect(destination.prefixes).toContain(destination.path);
    }
  });
});
