export const CONFIG = {
  API_BASE_URL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  CHAIN_ID: import.meta.env.VITE_CHAIN_ID || '31337',
  CONTRACT_ADDRESS: import.meta.env.VITE_CONTRACT_ADDRESS || '0x5FbDB2315678afecb367f032d93F642f64180aa3',
  JWT_STORAGE_KEY: 'ehealth_jwt_token'
};
