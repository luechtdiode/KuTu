"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[9946],{9946:(D,p,c)=>{c.r(p),c.d(p,{LastResultsPageModule:()=>B});var h=c(6895),m=c(4719),x=c(229),o=c(9928),T=c(5861),f=c(2198),_=c(4850),v=c(13),b=c(5778),S=c(2474),A=c(7545),C=c(7225),Z=c(5529),L=c(591),I=c(1086),e=c(5675),y=c(600),M=c(2997);function k(n,a){if(1&n&&(e.TgZ(0,"em"),e._uU(1),e.qZA()),2&n){const t=e.oxw();e.xp6(1),e.hij("\xf8 Ger\xe4t ",t.teilnehmer["\xf8 Ger\xe4t"],"")}}function P(n,a){if(1&n&&(e.TgZ(0,"em"),e._uU(1),e.qZA()),2&n){const t=e.oxw();e.xp6(1),e.AsE("Total D/E ",t.teilnehmer["Total D"],"/",t.teilnehmer["Total E"],"")}}let R=(()=>{class n{constructor(){this.knownKeys=["athletID","Rang","Athlet","Jahrgang","Verein","\xf8 Ger\xe4t","Total D","Total E","Total Punkte"],this.teilnehmerSubResults=""}ngOnInit(){this.teilnehmerSubResults=Object.keys(this.teilnehmer).map(t=>`${t}`).filter(t=>this.knownKeys.indexOf(t)<0).map(t=>`${t}: ${this.teilnehmer[t].Endnote} (${this.teilnehmer[t].Rang}.)`).join(", ")}}return n.\u0275fac=function(t){return new(t||n)},n.\u0275cmp=e.Xpm({type:n,selectors:[["scorelist-item"]],inputs:{teilnehmer:"teilnehmer"},outputs:{selected:"selected"},decls:18,vars:8,consts:[[3,"click"],["slot","start"],["src","assets/imgs/athlete.png"],["slot","end"],[4,"ngIf"]],template:function(t,i){1&t&&(e.TgZ(0,"ion-item",0),e.NdJ("click",function(){return i.selected?i.selected.emit(i.teilnehmer):{}}),e.TgZ(1,"ion-avatar",1),e._UZ(2,"img",2),e.qZA(),e.TgZ(3,"ion-label"),e._uU(4),e.TgZ(5,"small"),e._uU(6),e.qZA(),e._UZ(7,"br"),e._uU(8," \xa0\xa0\xa0\xa0\xa0\xa0"),e.TgZ(9,"small")(10,"em"),e._uU(11),e.qZA()()(),e.TgZ(12,"ion-note",3)(13,"b"),e._uU(14),e.qZA(),e._UZ(15,"br"),e.YNc(16,k,2,1,"em",4),e.YNc(17,P,2,2,"em",4),e.qZA()()),2&t&&(e.xp6(4),e.lnq("",i.teilnehmer.Rang,". ",i.teilnehmer.Athlet," (",i.teilnehmer.Jahrgang,")\xa0"),e.xp6(2),e.hij(" - ",i.teilnehmer.Verein,""),e.xp6(5),e.Oqu(i.teilnehmerSubResults),e.xp6(3),e.hij("Total Punkte ",i.teilnehmer["Total Punkte"],""),e.xp6(2),e.Q6J("ngIf",i.teilnehmer["\xf8 Ger\xe4t"]),e.xp6(1),e.Q6J("ngIf",i.teilnehmer["Total D"]))},dependencies:[h.O5,o.BJ,o.Ie,o.Q$,o.uN],styles:["ion-note[_ngcontent-%COMP%]{text-align:end}"]}),n})();function w(n,a){if(1&n&&(e.TgZ(0,"ion-select-option",10),e._uU(1),e.ALo(2,"date"),e.qZA()),2&n){const t=a.$implicit;e.Q6J("value",t.uuid),e.xp6(1),e.hij(" ",t.titel+" "+e.xi3(2,2,t.datum,"dd-MM-yy"),"")}}function F(n,a){if(1&n){const t=e.EpF();e.TgZ(0,"ion-select",8),e.NdJ("ngModelChange",function(s){e.CHM(t);const l=e.oxw(2);return e.KtG(l.competition=s)}),e.YNc(1,w,3,5,"ion-select-option",9),e.qZA()}if(2&n){const t=e.oxw(2);e.Q6J("ngModel",t.competition),e.xp6(1),e.Q6J("ngForOf",t.getCompetitions())}}function J(n,a){if(1&n&&(e.TgZ(0,"ion-col")(1,"ion-item")(2,"ion-label"),e._uU(3,"Wettkampf"),e.qZA(),e.YNc(4,F,2,2,"ion-select",7),e.qZA()()),2&n){const t=e.oxw();e.xp6(4),e.Q6J("ngIf",t.getCompetitions().length>0)}}function O(n,a){if(1&n&&(e.TgZ(0,"ion-note",11),e._uU(1),e.qZA()),2&n){const t=e.oxw();e.xp6(1),e.hij(" ",t.competitionName()," ")}}function N(n,a){if(1&n){const t=e.EpF();e.TgZ(0,"ion-searchbar",12),e.NdJ("ngModelChange",function(s){e.CHM(t);const l=e.oxw();return e.KtG(l.sMyQuery=s)})("ionInput",function(s){e.CHM(t);const l=e.oxw();return e.KtG(l.reloadList(s))})("ionCancel",function(s){e.CHM(t);const l=e.oxw();return e.KtG(l.reloadList(s))}),e.qZA()}if(2&n){const t=e.oxw();e.Q6J("ngModel",t.sMyQuery)}}const Q=function(n){return{"gradient-border":n}};function U(n,a){if(1&n&&(e.TgZ(0,"ion-col",14)(1,"div",15),e._UZ(2,"result-display",16),e.qZA()()),2&n){const t=a.$implicit,i=e.oxw(2);e.xp6(1),e.Q6J("ngClass",e.VKq(3,Q,i.isNew(t))),e.xp6(1),e.Q6J("item",t)("title",i.getTitle(t))}}function G(n,a){if(1&n&&(e.TgZ(0,"ion-grid",2)(1,"ion-row"),e.YNc(2,U,3,5,"ion-col",13),e.qZA()()),2&n){const t=e.oxw();e.xp6(2),e.Q6J("ngForOf",t.items)}}function $(n,a){if(1&n){const t=e.EpF();e.TgZ(0,"ion-item-sliding",null,18)(2,"scorelist-item",19),e.NdJ("click",function(){const l=e.CHM(t).$implicit,r=e.MAs(1),u=e.oxw(3);return e.KtG(u.scoreItemTapped(l,r))}),e.qZA(),e.TgZ(3,"ion-item-options",20)(4,"ion-item-option",21),e.NdJ("click",function(){const l=e.CHM(t).$implicit,r=e.MAs(1),u=e.oxw(3);return e.KtG(u.followAthlet(l,r))}),e._UZ(5,"ion-icon",22),e._uU(6),e.qZA()()()}if(2&n){const t=a.$implicit;e.xp6(2),e.Q6J("teilnehmer",t),e.xp6(4),e.hij(" Detail-Wertungen ",t.Athlet," ")}}function q(n,a){if(1&n&&(e.TgZ(0,"div")(1,"ion-item-divider"),e._uU(2),e.qZA(),e.YNc(3,$,7,2,"ion-item-sliding",17),e.qZA()),2&n){const t=a.$implicit;e.xp6(2),e.Oqu(t.title.text),e.xp6(1),e.Q6J("ngForOf",t.rows)}}function z(n,a){if(1&n&&(e.TgZ(0,"ion-list"),e.YNc(1,q,4,2,"div",17),e.qZA()),2&n){const t=e.oxw();e.xp6(1),e.Q6J("ngForOf",t.filteredScoreList)}}function Y(n,a){if(1&n){const t=e.EpF();e.TgZ(0,"ion-footer")(1,"ion-toolbar")(2,"ion-button",23),e.NdJ("click",function(){e.CHM(t);const s=e.oxw();return e.KtG(s.presentActionSheet())}),e._UZ(3,"ion-icon",24),e.qZA()()()}}let j=(()=>{class n{constructor(t,i,s){this.navCtrl=t,this.backendService=i,this.actionSheetController=s,this.items=[],this.geraete=[],this.scorelinks=[],this.defaultPath=void 0,this.scoreblocks=[],this.sFilteredScoreList=[],this.tMyQueryStream=new Z.xQ,this.sFilterTask=void 0,this.busy=new L.X(!1),this._title="Aktuelle Resultate",this.backendService.competitions||this.backendService.getCompetitions()}ngOnInit(){this.backendService.activateNonCaptionMode(this.backendService.competition).subscribe(t=>{this.geraete=t||[],this.sortItems()}),this.backendService.newLastResults.pipe((0,f.h)(t=>!!t&&!!t.results)).subscribe(t=>{this.lastItems=this.items.map(i=>i.id*this.geraete.length+i.geraet),this.items=[],Object.keys(t.results).forEach(i=>{this.items.push(t.results[i])}),this.sortItems(),this.scorelistAvailable()?this.backendService.getScoreLists().subscribe(i=>{const l=`/api/scores/${this.competitionContainer().uuid}/query?groupby=Kategorie:Geschlecht`;if(i){const r=Object.values(i);this.scorelinks=[...r.filter(g=>"Zwischenresultate"!=g.name),{name:"Generische Rangliste",published:!0,"published-date":"","scores-href":l,"scores-query":l}];const u=this.scorelinks.filter(g=>""+g.published=="true");this.refreshScoreList(u[0])}}):this.title="Aktuelle Resultate"})}get title(){return this._title}set title(t){this._title=t}refreshScoreList(t){let i=t["scores-href"];t.published||(i=t["scores-query"]),i.startsWith("/")&&(i=i.substring(1)),i=i.replace("html",""),this.title=t.name,this.defaultPath=i,this.backendService.getScoreList(this.defaultPath).pipe((0,_.U)(s=>s.title&&s.scoreblocks?s.scoreblocks:[])).subscribe(s=>{this.scoreblocks=s;const l=this.tMyQueryStream.pipe((0,f.h)(r=>!!r&&!!r.target&&!!r.target.value),(0,_.U)(r=>r.target.value),(0,v.b)(1e3),(0,b.x)(),(0,S.B)());l.subscribe(r=>{this.busy.next(!0)}),l.pipe((0,A.w)(this.runQuery(s))).subscribe(r=>{this.sFilteredScoreList=r,this.busy.next(!1)})})}sortItems(){this.items=this.items.sort((t,i)=>{let s=t.programm.localeCompare(i.programm);return 0===s&&(s=this.geraetOrder(t.geraet)-this.geraetOrder(i.geraet)),s}).filter(t=>void 0!==t.wertung.endnote)}isNew(t){return 0===this.lastItems.filter(i=>i===t.id*this.geraete.length+t.geraet).length}get stationFreezed(){return this.backendService.stationFreezed}set competition(t){this.stationFreezed||(this.backendService.getDurchgaenge(t),this.backendService.activateNonCaptionMode(this.backendService.competition).subscribe(i=>{this.geraete=i||[],this.sortItems()}))}get competition(){return this.backendService.competition||""}getCompetitions(){return this.backendService.competitions||[]}competitionContainer(){const t={titel:"",datum:new Date,auszeichnung:void 0,auszeichnungendnote:void 0,id:void 0,uuid:void 0,programmId:0};if(!this.backendService.competitions)return t;const i=this.backendService.competitions.filter(s=>s.uuid===this.backendService.competition);return 1===i.length?i[0]:t}competitionName(){const t=this.competitionContainer();return""===t.titel?"":t.titel+", am "+(t.datum+"T").split("T")[0].split("-").reverse().join("-")}geraetOrder(t){return this.geraete?this.geraete.findIndex(s=>s.id===t):0}geraetText(t){if(!this.geraete)return"";const i=this.geraete.filter(s=>s.id===t).map(s=>s.name);return 1===i.length?i[0]:""}getTitle(t){return t.programm+" - "+this.geraetText(t.geraet)}scorelistAvailable(){return!!this.items&&0===this.items.length&&new Date(this.competitionContainer().datum).getTime()<new Date(Date.now()-864e5).getTime()}get filteredScoreList(){return this.sFilteredScoreList&&this.sFilteredScoreList.length>0?this.sFilteredScoreList:this.getScoreListItems()}get isBusy(){return this.busy}runQuery(t){return i=>{const s=i.trim();let l=[];return s&&t&&t.forEach(r=>{const u=this.filter(s),g=r.rows.filter(d=>u(d,r));if(g.length>0){const d={title:r.title,rows:g};l=[...l,d]}}),(0,I.of)(l)}}filter(t){const i=t.toUpperCase().split(" ");return(s,l)=>i.filter(r=>{if(s.Athlet.toUpperCase().indexOf(r)>-1||s.Verein.toUpperCase().indexOf(r)>-1||s.Jahrgang.toUpperCase().indexOf(r)>-1||l.title.text.toUpperCase().replace("."," ").replace(","," ").split(" ").indexOf(r)>-1)return!0}).length===i.length}reloadList(t){this.tMyQueryStream.next(t)}makeGenericScoreListLink(){return`${C.AC}${this.defaultPath}&html`}getScoreListItems(){return this.scoreblocks}scoreItemTapped(t,i){i.getOpenAmount().then(s=>{s>0?i.close():i.open("end")})}followAthlet(t,i){i.close(),this.navCtrl.navigateForward(`athlet-view/${this.backendService.competition}/${t.athletID}`)}open(){window.open(this.makeGenericScoreListLink(),"_blank")}isShareAvailable(){return!!navigator&&!!navigator.share}share(){let t="GeTu";const s=this.competitionContainer();""!==s.titel&&(t={1:"Athletik",11:"KuTu",20:"GeTu",31:"KuTu"}[s.programmId]);let l=`${this.competitionName()} #${t}-${s.titel.replace(","," ").split(" ").join("_")}`;this.isShareAvailable()&&navigator.share({title:`${t} Rangliste`,text:l,url:this.makeGenericScoreListLink()}).then(function(){console.log("Article shared")}).catch(function(r){console.log(r.message)})}presentActionSheet(){var t=this;return(0,T.Z)(function*(){let i=[...t.scorelinks.map(r=>""+r.published=="true"?{text:`${r.name} anzeigen ...`,icon:"document",handler:()=>{t.refreshScoreList(r)}}:{text:`${r.name} (unver\xf6ffentlicht)`,icon:"today-outline",handler:()=>{t.refreshScoreList(r)}}),{text:"Rangliste \xf6ffnen ...",icon:"open",handler:()=>{t.open()}}];t.isShareAvailable()&&(i=[...i,{text:"Teilen / Share ...",icon:"share",handler:()=>{t.share()}}]);let s={header:"Aktionen",cssClass:"my-actionsheet-class",buttons:i};yield(yield t.actionSheetController.create(s)).present()})()}}return n.\u0275fac=function(t){return new(t||n)(e.Y36(o.SH),e.Y36(y.v),e.Y36(o.BX))},n.\u0275cmp=e.Xpm({type:n,selectors:[["app-last-results"]],decls:19,vars:7,consts:[["slot","start"],["slot","icon-only","name","menu"],["no-padding",""],[4,"ngIf"],["slot","end",4,"ngIf"],["placeholder","Search","showCancelButton","never",3,"ngModel","ngModelChange","ionInput","ionCancel",4,"ngIf"],["no-padding","",4,"ngIf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],["slot","end"],["placeholder","Search","showCancelButton","never",3,"ngModel","ngModelChange","ionInput","ionCancel"],["class","align-self-start","size-xs","12","size-md","6","size-lg","4","size-xl","2","col-12","","col-sm-6","","col-lg-4","","col-xl-2","",4,"ngFor","ngForOf"],["size-xs","12","size-md","6","size-lg","4","size-xl","2","col-12","","col-sm-6","","col-lg-4","","col-xl-2","",1,"align-self-start"],[3,"ngClass"],[3,"item","title"],[4,"ngFor","ngForOf"],["slidingAthletScoreItem",""],[3,"teilnehmer","click"],["side","end"],["color","primary",3,"click"],["name","arrow-dropright-circle","ios","md-arrow-dropright-circle"],["size","large","expand","block","color","primary",3,"click"],["slot","start","name","list"]],template:function(t,i){1&t&&(e.TgZ(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",0)(3,"ion-menu-toggle")(4,"ion-button"),e._UZ(5,"ion-icon",1),e.qZA()()(),e.TgZ(6,"ion-title")(7,"ion-grid",2)(8,"ion-row")(9,"ion-col")(10,"ion-label"),e._uU(11),e.qZA()(),e.YNc(12,J,5,1,"ion-col",3),e.qZA()()(),e.YNc(13,O,2,1,"ion-note",4),e.qZA(),e.YNc(14,N,1,1,"ion-searchbar",5),e.qZA(),e.TgZ(15,"ion-content"),e.YNc(16,G,3,1,"ion-grid",6),e.YNc(17,z,2,1,"ion-list",3),e.qZA(),e.YNc(18,Y,4,0,"ion-footer",3)),2&t&&(e.xp6(11),e.Oqu(i.title),e.xp6(1),e.Q6J("ngIf",!i.competition),e.xp6(1),e.Q6J("ngIf",i.competition),e.xp6(1),e.Q6J("ngIf",i.scorelistAvailable()),e.xp6(2),e.Q6J("ngIf",i.items.length>0),e.xp6(1),e.Q6J("ngIf",i.scorelistAvailable()),e.xp6(1),e.Q6J("ngIf",i.scorelistAvailable()))},dependencies:[h.mk,h.sg,h.O5,m.JJ,m.On,o.YG,o.Sm,o.wI,o.W2,o.fr,o.jY,o.Gu,o.gu,o.Ie,o.rH,o.u8,o.IK,o.td,o.Q$,o.q_,o.zc,o.uN,o.Nd,o.VI,o.t9,o.n0,o.sr,o.wd,o.QI,o.j9,M.G,R,h.uU]}),n})();var E=c(5051);const K=[{path:"",component:j}];let B=(()=>{class n{}return n.\u0275fac=function(t){return new(t||n)},n.\u0275mod=e.oAB({type:n}),n.\u0275inj=e.cJS({imports:[h.ez,m.u5,o.Pc,x.Bz.forChild(K),E.K]}),n})()}}]);