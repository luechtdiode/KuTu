import{a as ne,b as oe,h as re}from"./chunk-5JK2MF46.js";import{$a as J,B as l,Bb as ie,C as o,D as g,E as y,Ea as W,H as c,Ja as j,Ka as A,Ma as D,Na as G,O as d,Pa as V,Q as f,Qa as $,Ra as q,T as C,Ta as U,U as M,V as k,Y as w,Ya as H,_ as R,ab as K,ba as z,ca as O,db as Q,e as h,ea as P,fb as X,ga as F,gb as Y,kb as Z,lb as ee,m as x,o as T,p as S,qa as E,sa as L,t as r,u as v,ub as te,v as I,va as B,w as b,x as m,y as u,z as a,za as N}from"./chunk-KMY4ZES7.js";import"./chunk-6IQPZO6F.js";import"./chunk-JSAAGAZE.js";import"./chunk-5XVA5RR2.js";import"./chunk-LOHYXAXQ.js";import"./chunk-P5VFIEEE.js";import"./chunk-HC6MZPB3.js";import"./chunk-J5OMF6R4.js";import"./chunk-KWW2H4SN.js";import"./chunk-7HA5TF5T.js";import"./chunk-ZOBGMWXG.js";import"./chunk-426OJ4HC.js";import"./chunk-RPQZFOYD.js";import"./chunk-GIGBYVJT.js";import"./chunk-MM5QLNJM.js";import"./chunk-H3GX5QFY.js";import"./chunk-7D6K5XYM.js";import"./chunk-OBXDPQ3V.js";import"./chunk-G2DZN4YG.js";import"./chunk-MCRJI3T3.js";import"./chunk-TI7J2N3O.js";import"./chunk-TTFYQ64X.js";import"./chunk-2R6CW7ES.js";function le(i,s){if(i&1&&(l(0,"ion-select-option",9),d(1),w(2,"date"),o()),i&2){let e=s.$implicit;a("value",e.uuid),r(),f(" ",e.titel+" "+R(2,2,e.datum,"dd-MM-yy"),"")}}function pe(i,s){if(i&1){let e=y();l(0,"ion-select",7),k("ngModelChange",function(n){T(e);let p=c(2);return M(p.competition,n)||(p.competition=n),S(n)}),m(1,le,3,5,"ion-select-option",8),o()}if(i&2){let e=c(2);C("ngModel",e.competition),r(),a("ngForOf",e.getCompetitions())}}function ce(i,s){if(i&1&&(l(0,"ion-col")(1,"ion-item"),m(2,pe,2,2,"ion-select",6),o()()),i&2){let e=c();r(2),a("ngIf",e.getCompetitions().length>0)}}function me(i,s){if(i&1&&(l(0,"ion-note",10),d(1),o()),i&2){let e=c();r(),f(" ",e.competitionName()," ")}}function ge(i,s){if(i&1&&(l(0,"ion-col",13),g(1,"result-display",14),o()),i&2){let e=s.$implicit,t=c(3);u("size-xl",t.getMaxColumnSpec()),r(),a("item",e)("title",t.getTitle(e))("groupedBy",t.groupBy.PROGRAMM)}}function de(i,s){if(i&1&&(l(0,"ion-row"),m(1,ge,2,4,"ion-col",12),o()),i&2){let e=s.$implicit,t=c(2);r(),a("ngForOf",t.getWertungen(e))}}function ue(i,s){if(i&1&&(l(0,"ion-grid",2),m(1,de,2,1,"ion-row",11),o()),i&2){let e=c();r(),a("ngForOf",e.getProgramme())}}function fe(i,s){if(i&1&&(l(0,"ion-col",16),g(1,"result-display",14),o()),i&2){let e=s.$implicit,t=c(3);u("size-xl",t.getMaxColumnSpec()),r(),a("item",e)("title",t.getTitle(e))("groupedBy",t.groupBy.PROGRAMM)}}function _e(i,s){if(i&1&&(l(0,"ion-row"),m(1,fe,2,4,"ion-col",15),o()),i&2){let e=s.$implicit,t=c(2);r(),a("ngForOf",t.getWertungen(e))}}function he(i,s){if(i&1&&(l(0,"ion-grid",2),m(1,_e,2,1,"ion-row",11),o()),i&2){let e=c();r(),a("ngForOf",e.getProgramme())}}var se=(()=>{class i{backendService;groupBy=ne;items=[];toptop={};geraete=[];ngOnInit(){this.backendService.competitionSubject.subscribe(e=>{this.backendService.activateNonCaptionMode(this.backendService.competition).subscribe(t=>{this.geraete=t||[],this.sortItems()}),this.backendService.newLastResults.pipe(h(t=>!!t&&!!t.lastTopResults)).subscribe(t=>{this.items=[],this.toptop={},Object.keys(t.lastTopResults).forEach(n=>{let p=t.lastTopResults[n],_=this.toptop[p.wertung.wettkampfdisziplinId];_?_.wertung.endnote<p.wertung.endnote&&(this.toptop[p.wertung.wettkampfdisziplinId]=p):this.toptop[p.wertung.wettkampfdisziplinId]=p}),Object.keys(this.toptop).forEach(n=>{let p=this.toptop[n];this.items.push(p)}),this.sortItems()})})}constructor(e){this.backendService=e,this.backendService.competitions||this.backendService.getCompetitions()}sortItems(){this.items=this.items.sort((e,t)=>{let n=e.programm.localeCompare(t.programm);return n===0&&(n=this.geraetOrder(e.geraet)-this.geraetOrder(t.geraet)),n})}get stationFreezed(){return this.backendService.stationFreezed}set competition(e){this.stationFreezed||(this.backendService.getDurchgaenge(e),this.backendService.activateNonCaptionMode(this.backendService.competition).subscribe(t=>{this.geraete=t||[],this.sortItems()}))}get competition(){return this.backendService.competition||""}getCompetitions(){return this.backendService.competitions||[]}competitionName(){if(!this.backendService.competitions)return"";let e=this.backendService.competitions.filter(t=>t.uuid===this.backendService.competition).map(t=>t.titel+", am "+(t.datum+"T").split("T")[0].split("-").reverse().join("-"));return e.length===1?e[0]:""}geraetOrder(e){return this.geraete?this.geraete.findIndex(n=>n.id===e):0}geraetText(e){if(!this.geraete)return"";let t=this.geraete.filter(n=>n.id===e).map(n=>n.name);return t.length===1?t[0]:""}getColumnSpec(){return this.geraete?.length||0}getMaxColumnSpec(){return Math.min(12,Math.max(1,Math.floor(12/this.geraete.length+.5)))}getTitle(e){return e.programm+" - "+this.geraetText(e.geraet)}onlyUnique(e,t,n){return n.indexOf(e)===t}getProgramme(){return this.items.map(e=>e.programm).filter(this.onlyUnique)}getWertungen(e){return this.items.filter(t=>t.programm===e)}itemTapped(e,t){}static \u0275fac=function(t){return new(t||i)(v(ie))};static \u0275cmp=I({type:i,selectors:[["app-last-top-results"]],standalone:!1,decls:17,vars:4,consts:[["slot","start"],["slot","icon-only","name","menu"],["no-padding",""],[4,"ngIf"],["slot","end",4,"ngIf"],["no-padding","",4,"ngIf"],["label","Wettkampf","placeholder","Bitte ausw\xE4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["label","Wettkampf","placeholder","Bitte ausw\xE4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModelChange","ngModel"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],["slot","end"],[4,"ngFor","ngForOf"],["class","align-self-start","size-xs","12","size-md","6","size-lg","3",4,"ngFor","ngForOf"],["size-xs","12","size-md","6","size-lg","3",1,"align-self-start"],[3,"item","title","groupedBy"],["class","align-self-start","size-xs","12","size-sm","6","size-md","4","size-lg","3",4,"ngFor","ngForOf"],["size-xs","12","size-sm","6","size-md","4","size-lg","3",1,"align-self-start"]],template:function(t,n){t&1&&(l(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",0)(3,"ion-menu-toggle")(4,"ion-button"),g(5,"ion-icon",1),o()()(),l(6,"ion-title")(7,"ion-grid",2)(8,"ion-row")(9,"ion-col")(10,"ion-label"),d(11,"Top Resultate"),o()(),m(12,ce,3,1,"ion-col",3),o()()(),m(13,me,2,1,"ion-note",4),o()(),l(14,"ion-content"),m(15,ue,2,1,"ion-grid",5)(16,he,2,1,"ion-grid",5),o()),t&2&&(r(12),a("ngIf",!n.competition),r(),a("ngIf",n.competition),r(2),a("ngIf",n.items.length>0&&n.getColumnSpec()===6),r(),a("ngIf",n.items.length>0&&n.getColumnSpec()!==6))},dependencies:[z,O,L,B,j,A,D,G,V,$,q,U,H,J,K,Q,X,Y,Z,ee,W,oe,P],encapsulation:2})}return i})();var xe=[{path:"",component:se}],Fe=(()=>{class i{static \u0275fac=function(t){return new(t||i)};static \u0275mod=b({type:i});static \u0275inj=x({imports:[F,N,te,E.forChild(xe),re]})}return i})();export{Fe as LastTopResultsPageModule};
