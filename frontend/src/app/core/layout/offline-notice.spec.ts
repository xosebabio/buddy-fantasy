import { render, screen } from '@testing-library/angular';

import { OfflineNotice } from './offline-notice';

describe('OfflineNotice', () => {
  let onLine: boolean;

  beforeEach(() => {
    onLine = true;
    vi.spyOn(navigator, 'onLine', 'get').mockImplementation(() => onLine);
  });

  it('says nothing while there is a network', async () => {
    await render(OfflineNotice);

    expect(screen.getByRole('status')).toBeEmptyDOMElement();
  });

  it('announces that there is no connection while the network is gone', async () => {
    const { fixture } = await render(OfflineNotice);

    onLine = false;
    window.dispatchEvent(new Event('offline'));
    await fixture.whenStable();

    expect(screen.getByRole('status')).toHaveTextContent(
      'Sin conexión. Podrás enviar cambios cuando vuelva la red.',
    );

    onLine = true;
    window.dispatchEvent(new Event('online'));
    await fixture.whenStable();

    expect(screen.getByRole('status')).toBeEmptyDOMElement();
  });
});
