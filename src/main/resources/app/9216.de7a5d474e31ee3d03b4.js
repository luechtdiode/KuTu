(self.webpackChunkresultcatcher=self.webpackChunkresultcatcher||[]).push([[9216],{9216:(e,t,i)=>{"use strict";i.r(t),i.d(t,{AthletViewPageModule:()=>b});var s=i(8583),r=i(665),n=i(2952),o=i(2859),a=i(5435),c=i(8002),l=i(8186),g=i(600),m=i(2997);function h(e,t){if(1&e&&(l.TgZ(0,"ion-note",5),l._uU(1),l.qZA()),2&e){const e=l.oxw();l.xp6(1),l.hij(" ",e.competitionName()," ")}}const u=function(e){return{"gradient-border":e}};function p(e,t){if(1&e&&(l.TgZ(0,"ion-col",6),l.TgZ(1,"div",7),l._UZ(2,"result-display",8),l.qZA(),l.qZA()),2&e){const e=t.$implicit,i=l.oxw();l.xp6(1),l.Q6J("ngClass",l.VKq(3,u,i.isNew(e))),l.xp6(1),l.Q6J("item",e)("title",i.getTitle(e))}}let d=(()=>{class e{constructor(e,t,i){this.navCtrl=e,this.route=t,this.backendService=i,this.items=[],this.lastItems=[],this.geraete=[]}makeItemHash(e){return e.id*this.geraete.length*200+100*e.geraet+(e.wertung.endnote||-1)}ngOnInit(){const e=parseInt(this.route.snapshot.paramMap.get("athletId")),t=this.route.snapshot.paramMap.get("wkId");this.backendService.loadAthletWertungen(t,e).pipe((0,a.h)(e=>!!e&&e.length>0)).subscribe(t=>{this.items=t,this.sortItems(e)}),this.backendService.geraeteSubject.subscribe(e=>{this.backendService.captionmode||(this.geraete=e)}),this.backendService.newLastResults.pipe((0,a.h)(e=>!!e),(0,c.U)(e=>e.results)).subscribe(e=>{this.lastItems=this.items.map(e=>this.makeItemHash(e)),this.items=this.items.map(t=>{const i=e[t.wertung.wettkampfdisziplinId];return i&&i.id===t.id?i:t})})}sortItems(e){this.items=this.items.sort((e,t)=>{let i=e.programm.localeCompare(t.programm);return 0===i&&(i=this.geraetOrder(e.geraet)-this.geraetOrder(t.geraet)),i})}isNew(e){const t=this.makeItemHash(e);return 0===this.lastItems.filter(e=>e===t).length}get stationFreezed(){return this.backendService.stationFreezed}get competition(){return this.backendService.competition||""}getCompetitions(){return this.backendService.competitions||[]}competitionName(){if(!this.backendService.competitions)return"";const e=this.backendService.competitions.filter(e=>e.uuid===this.backendService.competition).map(e=>e.titel+", am "+(e.datum+"T").split("T")[0].split("-").reverse().join("-"));return 1===e.length?e[0]:""}geraetOrder(e){return this.geraete?this.geraete.findIndex(t=>t.id===e):0}geraetText(e){if(!this.geraete)return"";const t=this.geraete.filter(t=>t.id===e).map(e=>e.name);return 1===t.length?t[0]:""}getTitle(e){return e.programm+" - "+this.geraetText(e.geraet)}}return e.\u0275fac=function(t){return new(t||e)(l.Y36(o.SH),l.Y36(n.gz),l.Y36(g.v))},e.\u0275cmp=l.Xpm({type:e,selectors:[["app-athlet-view"]],decls:15,vars:2,consts:[["slot","start"],["defaultHref","/"],["no-padding",""],["slot","end",4,"ngIf"],["class","align-self-start","size-xs","12","size-md","6","size-lg","4","size-xl","2","col-12","","col-sm-6","","col-lg-4","","col-xl-2","",4,"ngFor","ngForOf"],["slot","end"],["size-xs","12","size-md","6","size-lg","4","size-xl","2","col-12","","col-sm-6","","col-lg-4","","col-xl-2","",1,"align-self-start"],[3,"ngClass"],[3,"item","title"]],template:function(e,t){1&e&&(l.TgZ(0,"ion-header"),l.TgZ(1,"ion-toolbar"),l.TgZ(2,"ion-buttons",0),l._UZ(3,"ion-back-button",1),l.qZA(),l.TgZ(4,"ion-title"),l.TgZ(5,"ion-grid",2),l.TgZ(6,"ion-row"),l.TgZ(7,"ion-col"),l.TgZ(8,"ion-label"),l._uU(9,"Aktuelle Resultate"),l.qZA(),l.qZA(),l.qZA(),l.qZA(),l.qZA(),l.YNc(10,h,2,1,"ion-note",3),l.qZA(),l.qZA(),l.TgZ(11,"ion-content"),l.TgZ(12,"ion-grid",2),l.TgZ(13,"ion-row"),l.YNc(14,p,3,5,"ion-col",4),l.qZA(),l.qZA(),l.qZA()),2&e&&(l.xp6(10),l.Q6J("ngIf",t.competition),l.xp6(4),l.Q6J("ngForOf",t.items))},directives:[o.Gu,o.sr,o.Sm,o.oU,o.cs,o.wd,o.jY,o.Nd,o.wI,o.Q$,s.O5,o.W2,s.sg,o.uN,s.mk,m.G],styles:[""]}),e})();var Z=i(5051);const f=[{path:"",component:d}];let b=(()=>{class e{}return e.\u0275fac=function(t){return new(t||e)},e.\u0275mod=l.oAB({type:e}),e.\u0275inj=l.cJS({imports:[[s.ez,r.u5,o.Pc,n.Bz.forChild(f),Z.K]]}),e})()}}]);