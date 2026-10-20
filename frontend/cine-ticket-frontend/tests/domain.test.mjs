import test from 'node:test';
import assert from 'node:assert/strict';
import {filterMovies,bookableShowtimes,toggleSeat,bookingPayload,dateKey,timeLabel,safeUrl,esc} from '../src/domain.mjs';

test('tìm phim không dấu kết hợp trạng thái và thể loại',()=>{
 const movies=[{title:'Đảo Mây',status:'SHOWING',genres:[{genreId:4}]},{title:'Đảo Mây 2',status:'UPCOMING',genres:[{genreId:4}]}];
 assert.equal(filterMovies(movies,{query:'dao may',status:'SHOWING',genre:'4'}).length,1);
 assert.equal(filterMovies(movies,{query:'dao',genre:'2'}).length,0);
});
test('chỉ cho đặt suất SCHEDULED trong tương lai',()=>{
 const shows=[{showtimeId:1,status:'SCHEDULED',startTime:'2030-01-01T12:00:00Z'},{showtimeId:2,status:'CANCELLED',startTime:'2031-01-01T12:00:00Z'},{showtimeId:3,status:'SCHEDULED',startTime:'2020-01-01T12:00:00Z'}];
 assert.deepEqual(bookableShowtimes(shows,new Date('2029-01-01').getTime()).map(s=>s.showtimeId),[1]);
});
test('giờ và ngày được hiển thị theo Việt Nam',()=>{
 assert.equal(timeLabel('2030-01-01T18:30:00Z'),'01:30');
 assert.equal(dateKey('2030-01-01T18:30:00Z'),'2030-01-02');
});
test('không chọn ghế bận; giới hạn 8; bỏ chọn được',()=>{
 const seat={showtimeSeatId:5,status:'AVAILABLE'};
 assert.deepEqual(toggleSeat([],seat),[5]);assert.deepEqual(toggleSeat([5],seat),[]);
 assert.deepEqual(toggleSeat([],{...seat,status:'HOLDING'}),[]);
 assert.throws(()=>toggleSeat([1,2,3,4,6,7,8,9],seat),/8 ghế/);
});
test('gửi showtimeSeatId đúng, không tin giá do client truyền',()=>{
 const show={showtimeId:22,status:'SCHEDULED',startTime:'2099-01-01T12:00:00Z'}, seats=[{showtimeSeatId:901,seatId:3,status:'AVAILABLE'}];
 const p=bookingPayload({userId:7},show,seats,[901]);
 assert.deepEqual(p,{userId:7,showtimeId:22,showtimeSeatIds:[901],paymentMethod:'CASH'});
 assert.throws(()=>bookingPayload({userId:7},show,seats,[3]),/không còn trống/);
 assert.throws(()=>bookingPayload({userId:7},show,seats,[901,901]),/khác nhau/);
 assert.throws(()=>bookingPayload(null,show,seats,[901]),/khách hàng/);
});
test('escape nội dung backend và chặn URL thực thi mã',()=>{
 assert.equal(safeUrl('javascript:alert(1)'),'');assert.equal(safeUrl('data:text/html,abc'),'');
 assert.equal(safeUrl('https://example.com/trailer'),'https://example.com/trailer');
 assert.equal(esc('<img onerror="x">'),'&lt;img onerror=&quot;x&quot;&gt;');
});
