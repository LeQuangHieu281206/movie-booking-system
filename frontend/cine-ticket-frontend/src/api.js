const TOKEN_KEY='cineroom-access-token';
export function token(){return localStorage.getItem(TOKEN_KEY);}
export function saveToken(value){if(value)localStorage.setItem(TOKEN_KEY,value);else localStorage.removeItem(TOKEN_KEY);}
export async function request(endpoint,options={}){
  let response;
  const accessToken=token();
  try {response=await fetch('/api'+endpoint,{method:options.method||'GET',headers:{Accept:'application/json',...(options.body?{'Content-Type':'application/json'}:{}),...(accessToken?{Authorization:'Bearer '+accessToken}:{})},body:options.body?JSON.stringify(options.body):undefined,signal:AbortSignal.timeout(18000)});}
  catch {throw new Error('Không kết nối được backend. Kiểm tra cửa sổ backend rồi thử lại.');}
  const text=await response.text();let data;
  try{data=text?JSON.parse(text):null;}catch{data=null;}
  if(!response.ok){
    if(response.status===401){
      saveToken(null);saveProfile(null);
      if(!endpoint.startsWith('/auth/'))document.dispatchEvent(new Event('auth:unauthorized'));
    }
    throw new Error((typeof data?.message==='string'?data.message:null)||`Yêu cầu thất bại (HTTP ${response.status}). Kiểm tra dữ liệu và nhật ký backend.`);
  }
  return data;
}
export function profile(){try{return JSON.parse(localStorage.getItem('cineroom-profile'));}catch{return null;}}
export function saveProfile(value){if(value)localStorage.setItem('cineroom-profile',JSON.stringify(value));else localStorage.removeItem('cineroom-profile');}
export function rememberTicket(booking,showtime,movie,seats){
  try{
    const key='cineroom-tickets';const list=JSON.parse(localStorage.getItem(key)||'[]');
    list.unshift({booking,showtime,movie,seats});localStorage.setItem(key,JSON.stringify(list.slice(0,100)));
  }catch{/* A successful backend booking remains successful when browser storage is full. */}
}
export function ticketDetails(id){try{return JSON.parse(localStorage.getItem('cineroom-tickets')||'[]').find(t=>t.booking.bookingId===id);}catch{return null;}}
