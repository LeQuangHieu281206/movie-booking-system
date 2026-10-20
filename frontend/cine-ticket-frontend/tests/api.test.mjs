import test from 'node:test';
import assert from 'node:assert/strict';
import {request,setMode,saveProfile,profile,rememberTicket,ticketDetails} from '../src/api.js';
const local=new Map(),session=new Map();
globalThis.localStorage={getItem:k=>local.get(k)||null,setItem:(k,v)=>local.set(k,v),removeItem:k=>local.delete(k)};
globalThis.sessionStorage={getItem:k=>session.get(k)||null,setItem:(k,v)=>session.set(k,v),removeItem:k=>session.delete(k)};
test('API thật gửi đúng JSON; lỗi thiếu bridge rõ ràng và không fallback mẫu',async()=>{
 const original=globalThis.fetch;setMode('live');
 try{
  let sent;
  globalThis.fetch=async(url,options)=>{sent={url,options};return new Response('{"bookingId":7}',{status:200});};
  const result=await request('/frontend/createBooking',{method:'POST',body:{showtimeSeatIds:[23]}});
  assert.equal(result.bookingId,7);assert.equal(sent.url,'/api/frontend/createBooking');assert.equal(sent.options.method,'POST');assert.equal(JSON.parse(sent.options.body).showtimeSeatIds[0],23);
  globalThis.fetch=async()=>new Response('{}',{status:404});
  await assert.rejects(request('/frontend/getShowtimeSeats?showtimeId=1'),/Thiếu API bổ sung/);
  globalThis.fetch=async()=>new Response('{"message":"Ghế đã đặt"}',{status:409});
  await assert.rejects(request('/frontend/createBooking',{method:'POST',body:{}}),/Ghế đã đặt/);
  globalThis.fetch=async()=>{throw new TypeError('offline');};
  await assert.rejects(request('/getMovies'),/Không kết nối được backend/);
 }finally{globalThis.fetch=original;setMode('demo');}
});
test('hồ sơ và vé được tách riêng giữa chế độ mẫu và thật',()=>{
 setMode('demo');saveProfile({userId:1,fullName:'Mẫu'});
 rememberTicket({bookingId:8},{},{title:'Phim mẫu'},[]);
 setMode('live');assert.equal(profile(),null);assert.equal(ticketDetails(8),undefined);
 saveProfile({userId:7,fullName:'Thật'});setMode('demo');assert.equal(profile().userId,1);assert.equal(ticketDetails(8).movie.title,'Phim mẫu');
});
