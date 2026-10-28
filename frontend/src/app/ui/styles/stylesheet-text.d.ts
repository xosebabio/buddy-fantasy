// A stylesheet imported as text (`with { loader: 'text' }`), as the contrast test reads the tokens.
declare module '*.scss' {
  const text: string;
  export default text;
}
