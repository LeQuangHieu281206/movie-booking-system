export const money = value => new Intl.NumberFormat('vi-VN',{style:'currency',currency:'VND',maximumFractionDigits:0}).format(Number(value)||0);
export const dateKey = value => new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Ho_Chi_Minh',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date(value));
export const dayLabel = value => new Intl.DateTimeFormat('vi-VN',{timeZone:'Asia/Ho_Chi_Minh',weekday:'short',day:'2-digit',month:'2-digit'}).format(new Date(value));
export const timeLabel = value => new Intl.DateTimeFormat('vi-VN',{timeZone:'Asia/Ho_Chi_Minh',hour:'2-digit',minute:'2-digit',hour12:false}).format(new Date(value));
export const fold = text => String(text||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/đ/g,'d').replace(/Đ/g,'D').toLowerCase();
export const esc = value => String(value??'').replace(/[&<>"']/g,char=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
export function safeUrl(value) {try {const u=new URL(value);return ['https:','http:'].includes(u.protocol)?u.href:'';}catch{return '';}}
export function bookableShowtimes(items, now=Date.now()) {return items.filter(s=>s.status==='SCHEDULED'&&new Date(s.startTime).getTime()>now).sort((a,b)=>new Date(a.startTime)-new Date(b.startTime));}
export function filterMovies(items,{query='',status='',genre=''}={}) {return items.filter(m=>(!query||fold(m.title).includes(fold(query)))&&(!status||m.status===status)&&(!genre||(m.genres||[]).some(g=>String(g.genreId)===String(genre))));}
export function toggleSeat(ids, seat, max=8) {if(seat.status!=='AVAILABLE')return ids;if(ids.includes(seat.showtimeSeatId))return ids.filter(id=>id!==seat.showtimeSeatId);if(ids.length>=max)throw new Error('Mỗi lần đặt tối đa 8 ghế.');return [...ids,seat.showtimeSeatId];}
export function bookingPayload(user,showtime,seats,ids) {
  if(!user?.userId)throw new Error('Hãy thêm thông tin khách hàng trước khi đặt.');
  if(!ids.length||ids.length>8||new Set(ids).size!==ids.length)throw new Error('Hãy chọn từ 1 đến 8 ghế khác nhau.');
  if(!bookableShowtimes([showtime]).length)throw new Error('Suất chiếu đã bắt đầu hoặc không còn nhận đặt vé.');
  if(ids.some(id=>!seats.some(s=>s.showtimeSeatId===id&&s.status==='AVAILABLE')))throw new Error('Một ghế bạn chọn không còn trống. Hãy chọn lại.');
  return {userId:user.userId,showtimeId:showtime.showtimeId,showtimeSeatIds:ids,paymentMethod:'CASH'};
}
export function labelSeat(s){return `${s.seatRow}${s.seatNumber}`;}
