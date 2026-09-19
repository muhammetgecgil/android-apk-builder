/* Small, theme-aware vector illustrations. No polling, animation or DOM observers. */
(()=>{'use strict';
const drawings={
 dna:(m,a,g)=>`<g transform="rotate(-18 32 32)">
  <path d="M20 8c0 12 24 12 24 24S20 44 20 56" stroke="${a}" stroke-width="4.6" opacity=".64"/>
  <g stroke="${m}" stroke-width="1.7" opacity=".72">
   <path d="m21 12 21 3m-17 4 12 3m-9 5 9 2m-17 5 23 3m-18 5 14 3m-13 5 13 3m-18 3 20 3"/>
  </g>
  <path d="M44 8c0 12-24 12-24 24s24 12 24 24" stroke="${m}" stroke-width="4.6"/>
  <path d="M43 8c0 12-24 12-24 24s24 12 24 24" stroke="#fff" stroke-width=".8" opacity=".65"/>
  <path d="M20 8c0 6 6 9 12 12M44 32c0 6-6 9-12 12" stroke="${a}" stroke-width="4.6"/>
  <g fill="${m}" stroke="none"><circle cx="20" cy="8" r="2.8"/><circle cx="44" cy="8" r="2.8"/><circle cx="20" cy="32" r="2.8"/><circle cx="44" cy="32" r="2.8"/><circle cx="20" cy="56" r="2.8"/><circle cx="44" cy="56" r="2.8"/></g>
 </g>`,
 hourglass:(m,a,g)=>`<ellipse cx="32" cy="55" rx="19" ry="3" fill="#000" opacity=".3"/>
  <path d="M17 12v40m30-40v40" stroke="${m}" stroke-width="2.5" opacity=".58"/>
  <path d="M22 13h20c0 10-3 12-8 17v4c5 5 8 7 8 17H22c0-10 3-12 8-17v-4c-5-5-8-7-8-17Z" fill="${g}" stroke="${m}" stroke-width="1.65"/>
  <path d="M24 22h16c-1 3-4 5-8 9-4-4-7-6-8-9Z" fill="${a}" stroke="none"/>
  <ellipse cx="32" cy="22" rx="8" ry="1.5" fill="${m}" opacity=".85"/>
  <path d="M24 49c1-3 5-4 8-9 3 5 7 6 8 9Z" fill="${a}" stroke="none"/>
  <path d="M32 33v2m0 2v1" stroke="${m}" stroke-width="1.6"/>
  <path d="M25 15c0 5 1 8 3 10m-4 18 2-4" stroke="#fff" stroke-width="1.3" opacity=".8"/>
  <rect x="14" y="9" width="36" height="5" rx="2.5" fill="${m}" stroke="none"/>
  <rect x="14" y="51" width="36" height="5" rx="2.5" fill="${m}" stroke="none"/>
  <path d="M18 10h28M18 52h28" stroke="#fff" stroke-width=".8" opacity=".7"/>`,
 wave:(m,a,g)=>`<g transform="rotate(-9 32 32)"><rect x="12" y="9" width="38" height="42" rx="6" fill="${g}" stroke="${a}" stroke-width="1.5"/><path d="M18 15h17" stroke="${m}" stroke-width="1.6"/></g>
  <circle cx="34" cy="34" r="21" fill="#101019" stroke="${m}" stroke-width="1.9"/>
  <g stroke="${m}" stroke-width=".7" opacity=".32"><circle cx="34" cy="34" r="16.5"/><circle cx="34" cy="34" r="13.5"/><circle cx="34" cy="34" r="10.5"/></g>
  <path d="M17 30a18 18 0 0 1 13-13m8 34a18 18 0 0 0 13-13" stroke="${m}" stroke-width="2" opacity=".65"/>
  <circle cx="34" cy="34" r="7" fill="${a}" stroke="${m}" stroke-width="1"/>
  <circle cx="34" cy="34" r="1.7" fill="#12101b" stroke="none"/>
  <path d="M11 45v6m4-10v14m4-9v7m4-4v5" stroke="${m}" stroke-width="2.3"/>`,
 alarm:(m,a,g)=>`<path d="M10 28h44M16 18l-3-3m35 3 3-3M32 8v5" stroke="${a}" stroke-width="1.7" opacity=".8"/>
  <path d="M23 23a9 9 0 0 1 18 0" fill="${a}" stroke="${m}" stroke-width="1.3"/>
  <path d="m17 47-3 7m33-7 3 7" stroke="${m}" stroke-width="3"/>
  <path d="m13 27 6-5m26 0 6 5" stroke="${m}" stroke-width="4"/>
  <circle cx="32" cy="37" r="18" fill="#15121c" stroke="${m}" stroke-width="2.5"/>
  <circle cx="32" cy="37" r="14.5" fill="${g}" stroke="${a}" stroke-width=".8"/>
  <path d="M32 25v2m0 20v2m-12-12h2m20 0h2" stroke="${m}" stroke-width="1.4"/>
  <path d="M32 29v8l7 4" stroke="${m}" stroke-width="2.1"/>
  <circle cx="32" cy="37" r="2.2" fill="${a}" stroke="none"/>`,
 globe:(m,a,g)=>`<circle cx="30" cy="31" r="22" fill="${g}" stroke="${m}" stroke-width="1.9"/>
  <ellipse cx="30" cy="31" rx="10" ry="22" stroke="${m}" stroke-width="1.2" opacity=".72"/>
  <path d="M8 31h44M12 19c10 5 26 5 36 0M12 43c10-5 26-5 36 0" stroke="${m}" stroke-width="1.2" opacity=".6"/>
  <path d="M16 16a20 20 0 0 1 12-5" stroke="#fff" stroke-width="1.4" opacity=".7"/>
  <circle cx="44" cy="41" r="11" fill="#16101b" stroke="${a}" stroke-width="1.5"/>
  <path d="M44 46v-9m-5 1a6 6 0 0 1 10 0m-12-3a9 9 0 0 1 14 0" stroke="${m}" stroke-width="1.7"/>
  <circle cx="44" cy="35" r="1.7" fill="${a}" stroke="none"/>`,
 waves:(m,a,g)=>`<path d="M13 35v-5a19 19 0 0 1 38 0v5" stroke="${a}" stroke-width="5.5"/>
  <path d="M14 27a18 18 0 0 1 36 0" stroke="${m}" stroke-width="2.4"/>
  <rect x="9" y="31" width="12" height="21" rx="5" fill="${g}" stroke="${m}" stroke-width="1.8"/>
  <rect x="43" y="31" width="12" height="21" rx="5" fill="${g}" stroke="${m}" stroke-width="1.8"/>
  <path d="M18 35v13m28-13v13" stroke="${a}" stroke-width="2.5"/>
  <path d="M26 34v9m6-15v21m6-15v9" stroke="${m}" stroke-width="2.7"/>
  <path d="M49 52c-2 4-7 5-13 5" stroke="${m}" stroke-width="1.4" opacity=".7"/>`,
 target:(m,a,g)=>`<rect x="10" y="10" width="44" height="44" rx="12" fill="${g}" stroke="${m}" stroke-width="1.5"/>
  <path d="M21 20v24m11-24v24m11-24v24" stroke="${m}" stroke-width="1.2" opacity=".6"/>
  <rect x="17" y="25" width="8" height="10" rx="3" fill="${a}" stroke="${m}" stroke-width="1"/>
  <rect x="28" y="35" width="8" height="10" rx="3" fill="${m}" stroke="none"/>
  <rect x="39" y="18" width="8" height="10" rx="3" fill="${a}" stroke="${m}" stroke-width="1"/>
  <path d="M17 13h19" stroke="#fff" stroke-width=".8" opacity=".6"/>`
};
window.trPremiumArt=(name,instance)=>{
 const draw=drawings[name];if(!draw)return '';
 const id='tr2873-'+String(instance).replace(/[^a-zA-Z0-9_-]/g,'');
 const m=`url(#${id}-metal)`,a=`url(#${id}-accent)`,g=`url(#${id}-glass)`;
 return `<svg class="tr-premium-art" data-art="${name}" viewBox="0 0 64 64" aria-hidden="true" focusable="false"><defs>
  <linearGradient id="${id}-metal" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#fffafc"/><stop offset=".38" stop-color="#eadce2"/><stop offset=".65" stop-color="var(--art-shade,#ad7589)"/><stop offset="1" stop-color="#fff2f7"/></linearGradient>
  <linearGradient id="${id}-accent" x1="0" y1="0" x2="1" y2="1"><stop stop-color="var(--art-light,#ffa7b6)"/><stop offset=".46" stop-color="var(--art-accent,#ff3647)"/><stop offset="1" stop-color="var(--art-dark,#95273b)"/></linearGradient>
  <linearGradient id="${id}-glass" x1="0" y1="0" x2="1" y2=".7"><stop stop-color="#fff" stop-opacity=".21"/><stop offset=".48" stop-color="var(--art-accent,#ff3647)" stop-opacity=".035"/><stop offset="1" stop-color="#fff" stop-opacity=".13"/></linearGradient>
 </defs><g fill="none" stroke="none" stroke-linecap="round" stroke-linejoin="round">${draw(m,a,g)}</g></svg>`;
};
})();
