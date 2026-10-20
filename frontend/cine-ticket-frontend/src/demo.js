import { bookingPayload } from './domain.mjs';
const KEY='cineroom-demo-v1';
export const genres=[{genreId:1,name:'Khoa học viễn tưởng'},{genreId:2,name:'Tình cảm'},{genreId:3,name:'Phiêu lưu'},{genreId:4,name:'Hoạt hình'},{genreId:5,name:'Giật gân'}];
export const movies=[
  {movieId:1,title:'Lối Về Sao Hỏa',description:'Khi tín hiệu cuối cùng từ Trái Đất biến mất, một phi hành gia phải lựa chọn giữa hành trình trở về và bí mật nằm dưới những cồn cát đỏ. Một chuyến đi về khoảng cách, ký ức và điều khiến chúng ta gọi một nơi là nhà.',durationMinutes:128,language:'Tiếng Việt',releaseDate:'2026-10-16',ageRestriction:'T13',status:'SHOWING',director:'Minh Trần',actors:'An Nguyễn, Hải Lê',genres:[genres[0],genres[2]],posterUrl:'/assets/mars.svg'},
  {movieId:2,title:'Sau Cơn Mưa',description:'Trong một hiệu sách nhỏ giữa thành phố, hai người xa lạ tìm thấy những lá thư chưa từng được gửi. Những ngày mưa đưa họ trở lại với ước mơ tưởng đã ngủ quên.',durationMinutes:105,language:'Tiếng Việt',ageRestriction:'T13',status:'SHOWING',director:'Linh Phạm',actors:'Mai Anh, Quang Vũ',genres:[genres[1]],posterUrl:'/assets/rain.svg'},
  {movieId:3,title:'Chuyến Tàu Đêm',description:'Một chuyến tàu không có trên bản đồ. Sáu hành khách và một bí mật phải được giải mã trước khi bình minh xuất hiện.',durationMinutes:116,language:'Tiếng Việt',ageRestriction:'T16',status:'SHOWING',director:'Đức Hoàng',actors:'Tuấn Lê, Ngọc Hà',genres:[genres[4]],posterUrl:'/assets/train.svg'},
  {movieId:4,title:'Đảo Mây',description:'Cô bé Mây cùng người bạn robot nhỏ lên đường tìm hòn đảo trôi giữa bầu trời. Cuộc phiêu lưu khiến cả hai hiểu rằng lòng can đảm có thể bắt đầu từ những điều rất bé.',durationMinutes:94,language:'Lồng tiếng Việt',ageRestriction:'P',status:'SHOWING',director:'Studio Mây',actors:'Lồng tiếng: Thu An, Bảo Nam',genres:[genres[3],genres[2]],posterUrl:'/assets/cloud.svg'},
  {movieId:5,title:'Phía Bên Kia Biển',description:'Một nhiếp ảnh gia trở về làng chài cũ và gặp lại người bạn thời thơ ấu. Giữa những con sóng, họ học cách nói lời tạm biệt với quá khứ.',durationMinutes:110,language:'Tiếng Việt',ageRestriction:'T13',status:'UPCOMING',director:'Hà Vũ',actors:'Khánh An, Duy Trần',genres:[genres[1]],posterUrl:'/assets/sea.svg'},
  {movieId:6,title:'Tín Hiệu 09',description:'Một trạm quan sát nhận được thông điệp từ chính tương lai của mình. Nhóm nghiên cứu chỉ có chín giờ để thay đổi một sự kiện chưa xảy ra.',durationMinutes:122,language:'Tiếng Việt',ageRestriction:'T16',status:'UPCOMING',director:'Nam Bùi',actors:'Đan Linh, Hoàng Sơn',genres:[genres[0],genres[4]],posterUrl:'/assets/signal.svg'}
];
export const cinemas=[{cinemaId:1,name:'CineRoom Cầu Giấy',city:'Hà Nội',address:'128 Xuân Thủy, Cầu Giấy, Hà Nội',phoneNumber:'024 3768 2026'},{cinemaId:2,name:'CineRoom Hà Đông',city:'Hà Nội',address:'48 Quang Trung, Hà Đông, Hà Nội',phoneNumber:'024 3355 2026'},{cinemaId:3,name:'CineRoom Quận 1',city:'TP. Hồ Chí Minh',address:'86 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh',phoneNumber:'028 3822 2026'}];
function seed(){
  const showtimes=[];
  for(let day=0;day<6;day++) for(const movie of movies.filter(m=>m.status==='SHOWING')) for(const cinema of cinemas) for(const hour of [15,19,21]){
    const d=new Date(); d.setUTCDate(d.getUTCDate()+day);d.setUTCHours(hour-7,day%2?15:30,0,0);
    if(d.getTime()<Date.now()+30*60*1000)continue;
    const roomId=cinema.cinemaId*10+movie.movieId;
    showtimes.push({showtimeId:showtimes.length+1,movieId:movie.movieId,roomId,roomName:`Phòng ${movie.movieId}`,roomType:movie.movieId===1?'IMAX':'2D',cinemaId:cinema.cinemaId,cinemaName:cinema.name,cinemaCity:cinema.city,cinemaAddress:cinema.address,startTime:d.toISOString(),endTime:new Date(+d+movie.durationMinutes*60000).toISOString(),basePrice:movie.movieId===1?110000:85000,status:'SCHEDULED'});
  }
  return {created:Date.now(),showtimes,bookings:[],users:[],booked:{},nextUser:1,nextBooking:1};
}
function read(){try{const saved=JSON.parse(localStorage.getItem(KEY));if(saved?.created&&Date.now()-saved.created<6*86400000)return saved;}catch{}const data=seed();write(data);return data;}
function write(data){localStorage.setItem(KEY,JSON.stringify(data));}
function seatsFor(data,id){
  if(!data.showtimes.some(s=>s.showtimeId===id))throw new Error('Không tìm thấy suất chiếu.');
  return Array.from({length:64},(_,i)=>({showtimeSeatId:id*100+i+1,seatId:i+1,seatRow:'ABCDEFGH'[Math.floor(i/8)],seatNumber:i%8+1,seatType:i>=24&&i<48?'VIP':'NORMAL',status:(data.booked[id]||[]).includes(id*100+i+1)||[10,11,27,35].includes(i)?'BOOKED':'AVAILABLE'}));
}
export async function demoRequest(endpoint,{method='GET',body}={}){
  const data=read(),url=new URL(endpoint,'http://demo'),p=url.searchParams;
  const id=Number(p.get('id'));
  switch(url.pathname){
    case '/getMovies':return structuredClone(movies);
    case '/getMovieById':{const m=movies.find(m=>m.movieId===id);if(!m)throw new Error('Không tìm thấy phim.');return structuredClone(m);}
    case '/getGenres':return genres;
    case '/getCinemas':return cinemas;
    case '/getShowtimes':return data.showtimes.filter(s=>!p.has('movieId')||s.movieId===Number(p.get('movieId')));
    case '/getShowtimeById':{const s=data.showtimes.find(s=>s.showtimeId===id);if(!s)throw new Error('Không tìm thấy suất chiếu.');return s;}
    case '/frontend/getShowtimeSeats':return seatsFor(data,Number(p.get('showtimeId')));
    case '/getUserById':{const u=data.users.find(u=>u.userId===id);if(!u)throw new Error('Không tìm thấy mã khách hàng.');return u;}
    case '/createUser':{
      if(method!=='POST')break;
      if(data.users.some(u=>u.email.toLowerCase()===body.email.toLowerCase()))throw new Error('Email đã được dùng trong dữ liệu mẫu. Dùng mã khách hàng hoặc email khác.');
      const user={userId:data.nextUser++,fullName:body.fullName,email:body.email,phoneNumber:body.phoneNumber,role:'CUSTOMER'};
      data.users.push(user);write(data);return user;
    }
    case '/getBookingsByUserId':return data.bookings.filter(b=>b.userId===Number(p.get('userId')));
    case '/frontend/createBooking':{
      if(method!=='POST')break;
      const user=data.users.find(u=>u.userId===body.userId),show=data.showtimes.find(s=>s.showtimeId===body.showtimeId);
      if(!show)throw new Error('Suất chiếu không tồn tại.');
      const seats=seatsFor(data,show.showtimeId);
      bookingPayload(user,show,seats,body.showtimeSeatIds);
      const booking={bookingId:data.nextBooking++,userId:user.userId,showtimeId:show.showtimeId,bookingCode:'DEMO-'+crypto.randomUUID().slice(0,8).toUpperCase(),totalAmount:Number(show.basePrice)*body.showtimeSeatIds.length,status:'PENDING',paymentMethod:'CASH',bookingTime:new Date().toISOString()};
      data.booked[show.showtimeId]=[...(data.booked[show.showtimeId]||[]),...body.showtimeSeatIds];data.bookings.unshift(booking);write(data);return booking;
    }
  }
  throw new Error('Chức năng mẫu chưa hỗ trợ yêu cầu này.');
}
export function resetDemo(){localStorage.removeItem(KEY);}
