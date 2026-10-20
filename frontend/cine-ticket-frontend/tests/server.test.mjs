import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import {createServer} from '../server.mjs';
const listen=server=>new Promise(resolve=>server.listen(0,'127.0.0.1',()=>resolve('http://127.0.0.1:'+server.address().port)));
const close=server=>new Promise(resolve=>server.close(resolve));

test('server cung cấp trang, module, CSS, ảnh; chặn source backend và đường dẫn vượt thư mục',async()=>{
 const server=createServer();const base=await listen(server);
 try{
  const index=await fetch(base);assert.equal(index.status,200);assert.match(await index.text(),/lang="vi"/);
  for(const file of ['/src/app.js','/src/api.js','/src/demo.js','/src/domain.mjs','/src/styles.css','/assets/mars.svg']){
   const r=await fetch(base+file);assert.equal(r.status,200,file);
   if(file.endsWith('.mjs'))assert.match(r.headers.get('content-type'),/javascript/);
  }
  for(const file of ['/server.mjs','/backend-bo-sung/tao-ghe-theo-suat.sql','/src/%2e%2e%2fserver.mjs','/assets/%2e%2e%2fpackage.json']){
   assert.ok([403,404].includes((await fetch(base+file)).status),file);
  }
 }finally{await close(server);}
});
test('proxy giữ method, query, body, status và thông báo lỗi từ backend',async()=>{
 const backend=http.createServer(async(req,res)=>{
  const chunks=[];for await(const c of req)chunks.push(c);
  res.writeHead(req.url.includes('fail')?409:200,{'Content-Type':'application/json'});
  res.end(JSON.stringify({method:req.method,url:req.url,body:Buffer.concat(chunks).toString(),message:'Ghế vừa được đặt'}));
 });
 const upstream=await listen(backend);const server=createServer({backend:upstream}),base=await listen(server);
 try{
  const response=await fetch(base+'/api/frontend/createBooking?x=7',{method:'POST',headers:{'Content-Type':'application/json'},body:'{"seat":9}'});
  const data=await response.json();assert.equal(data.method,'POST');assert.equal(data.url,'/api/frontend/createBooking?x=7');assert.equal(data.body,'{"seat":9}');
  const failure=await fetch(base+'/api/fail');assert.equal(failure.status,409);assert.equal((await failure.json()).message,'Ghế vừa được đặt');
 }finally{await close(server);await close(backend);}
});
test('backend không chạy trả lỗi 502, không trả dữ liệu mẫu',async()=>{
 const temporary=http.createServer();const upstream=await listen(temporary);await close(temporary);
 const server=createServer({backend:upstream}),base=await listen(server);
 try{const response=await fetch(base+'/api/getMovies');assert.equal(response.status,502);assert.match((await response.json()).message,/backend/);}finally{await close(server);}
});
