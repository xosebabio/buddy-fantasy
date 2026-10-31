/** A number as a Spanish decimal for a text field: «100,5». */
export function decimalText(value: number): string {
  return String(value).replace('.', ',');
}

/** A decimal typed in Spanish (comma or point, optional minus); `null` if it is not a number. */
export function parseDecimal(text: string): number | null {
  const normalized = text.trim().replace(',', '.').replace('−', '-');
  return /^-?\d+(\.\d+)?$/.test(normalized) ? Number(normalized) : null;
}
