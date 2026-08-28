import {Tag} from 'antd';
export function StatusTag({value}:{value?:string}){const v=value||'-'; const color=v==='SUCCESS'||v==='RESOLVED'?'success':v==='FAILED'||v==='OPEN'?'error':v==='RUNNING'||v==='RETRYING'?'processing':'default'; return <Tag color={color}>{v}</Tag>}
