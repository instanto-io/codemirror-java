import {build} from 'esbuild';
import {readFile,writeFile,mkdir,readdir} from 'node:fs/promises';
import {createHash} from 'node:crypto';
import path from 'node:path';
const out='codemirror-webjar/src/main/resources/META-INF/resources/webjars/codemirror-teavm/0.1.0';
await mkdir(out,{recursive:true});
const result=await build({entryPoints:['browser/editor.js'],bundle:true,format:'iife',globalName:'CodeMirrorTea',target:['es2020'],outfile:`${out}/codemirror-teavm.js`,metafile:true,legalComments:'inline'});
const packages=new Set(Object.keys(result.metafile.inputs).filter(p=>p.startsWith('node_modules/')).map(p=>p.split('/').slice(1,p.split('/')[1].startsWith('@')?3:2).join('/')));
let notices='CodeMirror Java browser bundle — third-party licenses\n\n';
const versions={};
for(const name of [...packages].sort()) {
 const root=path.join('node_modules',name);
 const pkg=JSON.parse(await readFile(path.join(root,'package.json'),'utf8'));
 const license=(await readdir(root)).find(f=>/^licen[sc]e(?:\.\w+)?$/i.test(f));
 if(!license) throw new Error(`Missing license: ${name}`);
 versions[name]={version:pkg.version,license:pkg.license,repository:pkg.repository};
 notices+=`\n=== ${name}@${pkg.version} (${pkg.license}) ===\n\n${await readFile(path.join(root,license),'utf8')}\n`;
}
await writeFile(`${out}/THIRD-PARTY-LICENSES.txt`,notices);
const bytes=await readFile(`${out}/codemirror-teavm.js`);
await writeFile(`${out}/manifest.json`,JSON.stringify({bundleVersion:'0.1.0',sha256:createHash('sha256').update(bytes).digest('hex'),packages:versions},null,2)+'\n');
console.log(`Bundled ${packages.size} packages, ${bytes.length} bytes; all license texts retained.`);
