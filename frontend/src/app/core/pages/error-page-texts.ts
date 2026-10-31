/** The page of a `403` (`forbidden`) or a `404` (`not-found`) that no page handled itself. */
export type ErrorKind = 'forbidden' | 'not-found';

/** The title, also the route's for the tab of the browser, and the message of each one. */
export const ERROR_PAGES: Record<ErrorKind, { title: string; message: string }> = {
  forbidden: {
    title: $localize`:@@core.errorPage.forbidden.title:Sin acceso`,
    message: $localize`:@@core.errorPage.forbidden.message:No puedes ver esta página. Si crees que deberías, pregunta a quien lleva la liga.`,
  },
  'not-found': {
    title: $localize`:@@core.errorPage.notFound.title:No encontrado`,
    message: $localize`:@@core.errorPage.notFound.message:Esta página no existe o ya no está disponible. Comprueba el enlace o vuelve al inicio.`,
  },
};
