import http from 'node:http';
import { readFile, stat } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = path.dirname(fileURLToPath(import.meta.url));
export function createServer({backend = process.env.BACKEND_URL || 'http://localhost:8080'} = {}) {
  const target = new URL(backend);
  if (!['http:', 'https:'].includes(target.protocol)) throw new Error('BACKEND_URL phải dùng http hoặc https');
  const mime = {'.html':'text/html; charset=utf-8','.css':'text/css; charset=utf-8','.js':'text/javascript; charset=utf-8','.mjs':'text/javascript; charset=utf-8','.svg':'image/svg+xml','.png':'image/png','.jpg':'image/jpeg'};
  return http.createServer(async (req, res) => {
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
    try {
      const url = new URL(req.url, 'http://localhost');
      if (url.pathname === '/config') {
        res.setHeader('Content-Type', 'application/json');
        return res.end(JSON.stringify({backend:target.origin}));
      }
      if (url.pathname.startsWith('/api/')) {
        if (!['GET','POST','PUT','DELETE'].includes(req.method)) {res.writeHead(405); return res.end();}
        const chunks=[]; let length=0;
        for await (const chunk of req) {length+=chunk.length; if(length>1024*1024){res.writeHead(413);return res.end();} chunks.push(chunk);}
        const upstream = await fetch(new URL(url.pathname+url.search, target.origin), {
          method:req.method,
          headers:{'Accept':'application/json',...(req.headers['content-type']?{'Content-Type':req.headers['content-type']}:{})},
          body:['GET','HEAD'].includes(req.method)?undefined:Buffer.concat(chunks),
          signal:AbortSignal.timeout(15000), redirect:'error'
        });
        res.writeHead(upstream.status, {'Content-Type':upstream.headers.get('content-type')||'application/json', 'Cache-Control':'no-store'});
        return res.end(Buffer.from(await upstream.arrayBuffer()));
      }
      if(req.method!=='GET'&&req.method!=='HEAD'){res.writeHead(405);return res.end();}
      const pathname=decodeURIComponent(url.pathname);
      // Only the public app is served. Backend sources, guides, tests and server stay private.
      if(pathname!=='/' && pathname!=='/index.html' && !/^\/(src|assets)\/[a-zA-Z0-9_./-]+$/.test(pathname)){res.writeHead(404);return res.end('Không tìm thấy');}
      const file=path.resolve(root, '.'+(pathname==='/'?'/index.html':pathname));
      if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}
      const relative=path.relative(root,file).split(path.sep).join('/');
      if(relative!=='index.html'&&!relative.startsWith('src/')&&!relative.startsWith('assets/')){res.writeHead(403);return res.end();}
      if(!(await stat(file)).isFile())throw new Error('NOT_FOUND');
      res.writeHead(200,{'Content-Type':mime[path.extname(file)]||'application/octet-stream','Cache-Control':'no-cache'});
      res.end(req.method==='HEAD'?undefined:await readFile(file));
    } catch(error) {
      if(res.headersSent) return res.end();
      const isApi=req.url.startsWith('/api/');
      res.writeHead(isApi?502:404,{'Content-Type':'application/json; charset=utf-8'});
      res.end(JSON.stringify({message:isApi?'Không kết nối được backend. Kiểm tra backend đang chạy và BACKEND_URL.':'Không tìm thấy tài nguyên.'}));
    }
  });
}
if(process.argv[1] && path.resolve(process.argv[1])===fileURLToPath(import.meta.url)) {
  const port=Number(process.env.PORT||5173);
  const server=createServer();
  server.on('error',error=>{console.error(error.code==='EADDRINUSE'?`Cổng ${port} đang được sử dụng. Đóng cửa sổ frontend cũ rồi thử lại.`:error.message);process.exitCode=1;});
  server.listen(port,'127.0.0.1',()=>console.log(`\nCineRoom: http://localhost:${port}\nBackend: ${process.env.BACKEND_URL||'http://localhost:8080'}\nNhấn Ctrl+C để dừng.\n`));
}
