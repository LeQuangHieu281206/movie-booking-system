import test from 'node:test';
import assert from 'node:assert/strict';
import {demoRequest,resetDemo} from '../src/demo.js';
const storage=new Map();
globalThis.localStorage={getItem:k=>storage.get(k)||null,setItem:(k,v)=>storage.set(k,v),removeItem:k=>storage.delete(k)};

test('luồng mẫu: hồ sơ → phim → suất → ghế → vé → lịch sử; không đặt trùng ghế',async()=>{
 resetDemo();const films=await demoRequest('/getMovies');assert.ok(films.length>=4);
 const show=(await demoRequest('/getShowtimes?movieId='+films[0].movieId))[0];assert.ok(show);
 const seats=await demoRequest('/frontend/getShowtimeSeats?showtimeId='+show.showtimeId);
 const chosen=seats.filter(s=>s.status==='AVAILABLE').slice(0,2);
 const user=await demoRequest('/createUser',{method:'POST',body:{fullName:'Khách thử',email:'test@example.com',password:'never-store-this',phoneNumber:'0901234567'}});
 assert.ok(!JSON.stringify([...storage.values()]).includes('never-store-this'));
 const payload={userId:user.userId,showtimeId:show.showtimeId,showtimeSeatIds:chosen.map(s=>s.showtimeSeatId),paymentMethod:'CASH'};
 const booking=await demoRequest('/frontend/createBooking',{method:'POST',body:payload});
 assert.equal(booking.status,'PENDING');assert.equal(booking.totalAmount,2*show.basePrice);assert.match(booking.bookingCode,/^DEMO-/);
 const after=await demoRequest('/frontend/getShowtimeSeats?showtimeId='+show.showtimeId);
 assert.ok(chosen.every(s=>after.find(a=>a.showtimeSeatId===s.showtimeSeatId).status==='BOOKED'));
 await assert.rejects(demoRequest('/frontend/createBooking',{method:'POST',body:payload}),/không còn trống/);
 assert.equal((await demoRequest('/getBookingsByUserId?userId='+user.userId)).length,1);
 assert.equal((await demoRequest('/getBookingsByUserId?userId=999')).length,0);
 await assert.rejects(demoRequest('/createUser',{method:'POST',body:{...user,email:'TEST@example.com'}}),/Email/);
});
