import { createContext, useContext } from 'react';
export const RegistryContext = createContext<() => void>(() => undefined);
export const useRegistry = () => useContext(RegistryContext);
