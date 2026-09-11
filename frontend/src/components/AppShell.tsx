import { NavLink, Outlet } from 'react-router-dom';
import { useState } from 'react';
import { RegistryModal } from './RegistryModal';
import { RegistryContext } from '../context/RegistryContext';

const links = [['/', 'Início'], ['/bills', 'Contas'], ['/cards', 'Cartões'], ['/transactions', 'Movimentações'], ['/settings', 'Configurações']];
export function AppShell() {
  const [registry, setRegistry] = useState(false);
  return <RegistryContext.Provider value={() => setRegistry(true)}><div className="app-shell"><aside className="sidebar"><span className="brand">Ninho</span><span className="brand-subtitle">finanças da casa</span><p className="nav-label">VISÃO GERAL</p><nav className="nav-list" aria-label="Navegação principal">{links.map(([to, label]) => <NavLink key={to} to={to} end={to === '/'} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}><span>{label}</span></NavLink>)}</nav></aside><main className="app-main"><div className="content"><Outlet /></div></main>{registry && <RegistryModal onClose={() => setRegistry(false)} />}</div></RegistryContext.Provider>;
}
