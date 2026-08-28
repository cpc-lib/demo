import axios from 'axios';
export const api=axios.create({baseURL:'/api',timeout:10000});
export const getDashboard=(date:string)=>api.get('/dashboard',{params:{date}}).then(r=>r.data);
export const getBatches=()=>api.get('/batches').then(r=>r.data);
export const getLedgers=(date?:string,userId?:string)=>api.get('/ledgers',{params:{date:date||undefined,userId:userId||undefined}}).then(r=>r.data);
export const getFailures=()=>api.get('/failures').then(r=>r.data);
export const retryFailure=(id:number)=>api.post(`/failures/${id}/retry`).then(r=>r.data);
export const getReconciliation=(date:string)=>api.get('/reconciliation',{params:{date}}).then(r=>r.data);
export const getMissing=(date:string)=>api.get('/reconciliation/missing',{params:{date}}).then(r=>r.data);
