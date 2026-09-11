import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from './components/AppShell';
import { AppProvider } from './context/AppContext';
import { ToastProvider } from './context/ToastContext';
import { BillsPage } from './pages/BillsPage';
import { CardsPage } from './pages/CardsPage';
import { DashboardPage } from './pages/DashboardPage';
import { SettingsPage } from './pages/SettingsPage';
import { TransactionsPage } from './pages/TransactionsPage';

export default function App(){return <BrowserRouter><AppProvider><ToastProvider><Routes><Route element={<AppShell/>}><Route index element={<DashboardPage/>}/><Route path="dashboard" element={<DashboardPage/>}/><Route path="bills" element={<BillsPage/>}/><Route path="cards" element={<CardsPage/>}/><Route path="transactions" element={<TransactionsPage/>}/><Route path="settings" element={<SettingsPage/>}/><Route path="*" element={<Navigate to="/" replace/>}/></Route></Routes></ToastProvider></AppProvider></BrowserRouter>}
