import {demoRequest} from './demo.js';
export function mode(){return localStorage.getItem('cineroom-mode')==='live'?'live':'demo';}
export function setMode(value){localStorage.setItem('cineroom-mode',value==='live'?'live':'demo');}
export async function request(endpoint,options={}){
  if(mode()==='demo')return demoRequest(endpoint,options);
  let response;
  try {response=await fetch('/api'+endpoint,{method:options.method||'GET',headers:{Accept:'application/json',...(options.body?{'Content-Type':'application/json'}:{})},body:options.body?JSON.stringify(options.body):undefined,signal:AbortSignal.timeout(18000)});}
  catch {throw new Error('Không kết nối được backend. Kiểm tra cửa sổ backend rồi thử lại.');}
  const text=await response.text();let data;
  try{data=text?JSON.parse(text):null;}catch{data=null;}
  if(!response.ok){
    if(response.status===404&&endpoint.startsWith('/frontend/'))throw new Error('Thiếu API bổ sung cho chọn ghế/đặt vé. Chép FrontendBridgeController.java theo HUONG-DAN.md rồi khởi động lại backend.');
    throw new Error((typeof data?.message==='string'?data.message:null)||`Yêu cầu thất bại (HTTP ${response.status}). Kiểm tra dữ liệu và nhật ký backend.`);
  }
  return data;
}
export function profile(){try{return JSON.parse(sessionStorage.getItem('cineroom-profile-'+mode()));}catch{return null;}}
export function saveProfile(value){if(value)sessionStorage.setItem('cineroom-profile-'+mode(),JSON.stringify(value));else sessionStorage.removeItem('cineroom-profile-'+mode());}
export function rememberTicket(booking,showtime,movie,seats){
  try{
    const key='cineroom-tickets-'+mode();const list=JSON.parse(localStorage.getItem(key)||'[]');
    list.unshift({booking,showtime,movie,seats});localStorage.setItem(key,JSON.stringify(list.slice(0,100)));
  }catch{/* A successful backend booking remains successful when browser storage is full. */}
}
export function ticketDetails(id){try{return JSON.parse(localStorage.getItem('cineroom-tickets-'+mode())||'[]').find(t=>t.booking.bookingId===id);}catch{return null;}}
