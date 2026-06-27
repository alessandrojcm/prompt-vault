export default {
  input: "../../openapi/api.yaml",
  output: "src/generated",
  plugins: [
    "@hey-api/client-ky",
    {
      name: "@hey-api/sdk",
      validator: true,
    },
    {
      name: "valibot",
      requests: true,
      responses: true,
      definitions: true,
    },
    {
      name: "@tanstack/react-query",
      queryOptions: true,
      mutationOptions: true,
      mutationKeys: true,
    },
  ],
};
