export default {
  input: '../../openapi/api.yaml',
  output: 'src/generated',
  plugins: [
    '@hey-api/client-ky',
    {
      name: '@tanstack/react-query',
      queryOptions: true,
      mutationOptions: true,
    },
  ],
}
