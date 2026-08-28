import {create} from 'zustand';
type Page='dashboard'|'batches'|'ledgers'|'failures'|'reconciliation';
type State={page:Page;setPage:(p:Page)=>void;};
export const useAppStore=create<State>((set)=>({page:'dashboard',setPage:(page)=>set({page})}));
