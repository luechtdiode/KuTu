"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[5201],{5201:(V,h,a)=>{a.r(h),a.d(h,{AthletViewPageModule:()=>C});var c=a(6895),p=a(4719),g=a(229),o=a(9928),m=a(2198),f=a(4850),v=a(7225),u=a(2997),t=a(5675),w=a(600);function x(n,l){if(1&n){const e=t.EpF();t.TgZ(0,"ion-button",6),t.NdJ("click",function(){t.CHM(e);const s=t.oxw();return t.KtG(s.share())}),t._UZ(1,"ion-icon",7),t.qZA()}}function A(n,l){if(1&n&&(t.TgZ(0,"ion-col")(1,"ion-note"),t._uU(2),t.qZA(),t.TgZ(3,"ion-label")(4,"div",8),t._uU(5),t.qZA(),t.TgZ(6,"div",9),t._uU(7),t.qZA()()()),2&n){const e=t.oxw();t.xp6(2),t.hij(" ",e.competitionName()," "),t.xp6(3),t.Oqu(e.athlet.vorname+" "+e.athlet.name),t.xp6(2),t.Oqu(e.athlet.verein)}}const d=function(n){return{"gradient-border":n}};function T(n,l){if(1&n&&(t.TgZ(0,"ion-col",11)(1,"div",12),t._UZ(2,"result-display",13),t.qZA()()),2&n){const e=l.$implicit,i=t.oxw(2);t.uIk("size-xl",i.getMaxColumnSpec()),t.xp6(1),t.Q6J("ngClass",t.VKq(5,d,i.isNew(e))),t.xp6(1),t.Q6J("item",e)("title",i.getTitle(e))("groupedBy",i.groupBy.ATHLET)}}function I(n,l){if(1&n&&(t.TgZ(0,"ion-row"),t.YNc(1,T,3,7,"ion-col",10),t.qZA()),2&n){const e=t.oxw();t.xp6(1),t.Q6J("ngForOf",e.items)}}function Z(n,l){if(1&n&&(t.TgZ(0,"ion-col",15)(1,"div",12),t._UZ(2,"result-display",13),t.qZA()()),2&n){const e=l.$implicit,i=t.oxw(2);t.uIk("size-xl",i.getMaxColumnSpec()),t.xp6(1),t.Q6J("ngClass",t.VKq(5,d,i.isNew(e))),t.xp6(1),t.Q6J("item",e)("title",i.getTitle(e))("groupedBy",i.groupBy.ATHLET)}}function S(n,l){if(1&n&&(t.TgZ(0,"ion-row"),t.YNc(1,Z,3,7,"ion-col",14),t.qZA()),2&n){const e=t.oxw();t.xp6(1),t.Q6J("ngForOf",e.items)}}let b=(()=>{class n{constructor(e,i,s){this.navCtrl=e,this.route=i,this.backendService=s,this.groupBy=u.X,this.items=[],this.lastItems=[],this.geraete=[],this.backendService.competitions||this.backendService.getCompetitions()}makeItemHash(e){return e.id*this.geraete.length*200+100*e.geraet+(e.wertung.endnote||-1)}ngOnInit(){this.athletId=parseInt(this.route.snapshot.paramMap.get("athletId")),this.wkId=this.route.snapshot.paramMap.get("wkId"),this.backendService.loadAthletWertungen(this.wkId,this.athletId).pipe((0,m.h)(i=>!!i&&i.length>0)).subscribe(i=>{this.items=i,this.sortItems(this.athletId)}),this.backendService.geraeteSubject.subscribe(i=>{this.backendService.captionmode||(this.geraete=i)}),this.backendService.newLastResults.pipe((0,m.h)(i=>!!i),(0,f.U)(i=>i.results)).subscribe(i=>{this.lastItems=this.items.map(s=>this.makeItemHash(s)),this.items=this.items.map(s=>{const r=i[s.wertung.wettkampfdisziplinId];return r&&r.id===s.id?r:s})})}sortItems(e){this.items=this.items.sort((i,s)=>{let r=i.programm.localeCompare(s.programm);return 0===r&&(r=this.geraetOrder(i.geraet)-this.geraetOrder(s.geraet)),r})}isNew(e){const i=this.makeItemHash(e);return 0===this.lastItems.filter(s=>s===i).length}get stationFreezed(){return this.backendService.stationFreezed}get competition(){return this.backendService.competition||""}getCompetitions(){return this.backendService.competitions||[]}competitionContainer(){const e={titel:"",datum:new Date,auszeichnung:void 0,auszeichnungendnote:void 0,id:void 0,uuid:void 0,programmId:0};if(!this.backendService.competitions)return e;const i=this.backendService.competitions.filter(s=>s.uuid===this.backendService.competition);return 1===i.length?i[0]:e}competitionName(){const e=this.competitionContainer();return""===e.titel?"":e.titel+", am "+(e.datum+"T").split("T")[0].split("-").reverse().join("-")}get athlet(){return this.items&&this.items.length>0?this.items[0]:{}}getColumnSpec(){return this.geraete.length}getMaxColumnSpec(){return Math.min(12,Math.max(1,Math.floor(12/this.items.length+.5)))}geraetOrder(e){return this.geraete?this.geraete.findIndex(s=>s.id===e):0}geraetText(e){if(!this.geraete)return"";const i=this.geraete.filter(s=>s.id===e).map(s=>s.name);return 1===i.length?i[0]:""}getTitle(e){return e.programm+" - "+this.geraetText(e.geraet)}isShareAvailable(){return!!navigator&&!!navigator.share}share(){let e="GeTu";const s=this.competitionContainer();""!==s.titel&&(e={1:"Athletik",11:"KuTu",20:"GeTu",31:"KuTu"}[s.programmId]);let r=`${this.items[0].vorname} ${this.items[0].name}, ${this.items[0].verein} #${e}-${s.titel.replace(","," ").split(" ").join("_")}_${this.items[0].programm} #${this.items[0].verein.replace(","," ").split(" ").join("_")}`;r=new Date(s.datum).getTime()>Date.now()?"Hoffe das Beste f\xfcr "+r:"Geschafft! Wettkampf Resultate von "+r,this.isShareAvailable()&&navigator.share({title:this.competitionName(),text:r,url:`${v.AC}athlet-view/${this.wkId}/${this.athletId}`}).then(function(){console.log("Article shared")}).catch(function(P){console.log(P.message)})}}return n.\u0275fac=function(e){return new(e||n)(t.Y36(o.SH),t.Y36(g.gz),t.Y36(w.v))},n.\u0275cmp=t.Xpm({type:n,selectors:[["app-athlet-view"]],decls:19,vars:4,consts:[["slot","start"],["defaultHref","/"],["no-padding",""],["slot","end"],["slot","end",3,"click",4,"ngIf"],[4,"ngIf"],["slot","end",3,"click"],["name","share"],[2,"font-size","16pt"],[2,"font-size","10pt"],["class","align-self-start","size-xs","12","size-md","6","size-lg","4",4,"ngFor","ngForOf"],["size-xs","12","size-md","6","size-lg","4",1,"align-self-start"],[3,"ngClass"],[3,"item","title","groupedBy"],["class","align-self-start","size-xs","12","size-md","6","size-lg","3",4,"ngFor","ngForOf"],["size-xs","12","size-md","6","size-lg","3",1,"align-self-start"]],template:function(e,i){1&e&&(t.TgZ(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",0),t._UZ(3,"ion-back-button",1),t.qZA(),t.TgZ(4,"ion-title")(5,"ion-grid",2)(6,"ion-row")(7,"ion-col")(8,"ion-label"),t._uU(9,"Aktuelle Resultate"),t.qZA()()()()(),t.TgZ(10,"ion-buttons",3),t.YNc(11,x,2,0,"ion-button",4),t.qZA()(),t.TgZ(12,"ion-grid",2)(13,"ion-row"),t.YNc(14,A,8,3,"ion-col",5),t.qZA()()(),t.TgZ(15,"ion-content")(16,"ion-grid",2),t.YNc(17,I,2,1,"ion-row",5),t.YNc(18,S,2,1,"ion-row",5),t.qZA()()),2&e&&(t.xp6(11),t.Q6J("ngIf",i.isShareAvailable()),t.xp6(3),t.Q6J("ngIf",i.competition),t.xp6(3),t.Q6J("ngIf",6===i.getColumnSpec()),t.xp6(1),t.Q6J("ngIf",6!==i.getColumnSpec()))},dependencies:[c.mk,c.sg,c.O5,o.oU,o.YG,o.Sm,o.wI,o.W2,o.jY,o.Gu,o.gu,o.Q$,o.uN,o.Nd,o.sr,o.wd,o.cs,u.G]}),n})();var k=a(5051);const z=[{path:"",component:b}];let C=(()=>{class n{}return n.\u0275fac=function(e){return new(e||n)},n.\u0275mod=t.oAB({type:n}),n.\u0275inj=t.cJS({imports:[c.ez,p.u5,o.Pc,g.Bz.forChild(z),k.K]}),n})()}}]);