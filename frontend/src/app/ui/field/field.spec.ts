import { render, screen } from '@testing-library/angular';

import { Field, FieldInput } from './field';

describe('Field', () => {
  it('labels its control', async () => {
    await render('<bf-field label="Nombre de la liga"><input bfInput type="text" /></bf-field>', {
      imports: [Field, FieldInput],
    });

    const input = screen.getByRole('textbox', { name: 'Nombre de la liga' });
    expect(input).toHaveClass('bf-input');
    expect(input).not.toHaveAttribute('aria-describedby');
    expect(input).not.toHaveAttribute('aria-invalid');
  });

  it('describes the control with the hint', async () => {
    await render(
      '<bf-field label="Nombre" hint="Lo verán todos los managers."><input bfInput /></bf-field>',
      { imports: [Field, FieldInput] },
    );

    expect(screen.getByRole('textbox', { name: 'Nombre' })).toHaveAccessibleDescription(
      'Lo verán todos los managers.',
    );
  });

  it('marks the control as invalid and describes it with the hint and the error', async () => {
    await render(
      `<bf-field label="Nombre" hint="Lo verán todos." [error]="error">
        <input bfInput />
      </bf-field>`,
      {
        imports: [Field, FieldInput],
        componentProperties: { error: 'El nombre de la liga está vacío o es demasiado largo.' },
      },
    );

    const input = screen.getByRole('textbox', { name: 'Nombre' });
    expect(input).toBeInvalid();
    expect(input).toHaveAccessibleDescription(
      'Lo verán todos. El nombre de la liga está vacío o es demasiado largo.',
    );
  });

  it('clears the error when the page does', async () => {
    const properties: { error: string | null } = { error: 'Error' };
    const { rerender } = await render(
      '<bf-field label="Nombre" [error]="error"><input bfInput /></bf-field>',
      { imports: [Field, FieldInput], componentProperties: properties },
    );

    await rerender({ componentProperties: { error: null } });

    const input = screen.getByRole('textbox', { name: 'Nombre' });
    expect(input).toBeValid();
    expect(screen.queryByText('Error')).not.toBeInTheDocument();
  });

  it('gives each field its own ids', async () => {
    await render(
      `<bf-field label="Uno"><input bfInput /></bf-field>
       <bf-field label="Dos"><textarea bfInput></textarea></bf-field>`,
      { imports: [Field, FieldInput] },
    );

    expect(screen.getByRole('textbox', { name: 'Uno' }).id).not.toBe(
      screen.getByRole('textbox', { name: 'Dos' }).id,
    );
  });
});
