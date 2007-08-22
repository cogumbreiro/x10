/*
 * (c) Copyright IBM Corporation 2007
 *
 * $Id: aggregate_hc.cc,v 1.8 2007-08-22 10:59:53 ganeshvb Exp $
 * This file is part of X10 Runtime System.
 */

/** Implementation file for inlinable asyncs aggregation. **/

#include <x10/aggregate.h>
#include <x10/xmacros.h>
#include <x10/xassert.h>
#include <stdarg.h>
#include <string.h>
#include <iostream>

using namespace x10lib;
using namespace std;


#define X10_MAX_LOG_NUMPROCS 8

#define MIN(A, B) A > B ? B : A

static char sbuf [2 * 32 * X10_MAX_AGG_SIZE * 8];

static char* rbuf[X10_MAX_LOG_NUMPROCS];

static char** __x10_agg_arg_buf[X10_MAX_AGG_HANDLERS];

static int* __x10_agg_counter[X10_MAX_AGG_HANDLERS];

static int __x10_agg_total[X10_MAX_AGG_HANDLERS];

size_t recvMesgLen[X10_MAX_LOG_NUMPROCS];

lapi_cntr_t recvCntr[X10_MAX_LOG_NUMPROCS];

typedef struct {
  ulong len;
  ulong phase;
} x10_agg_cmpl_t;

typedef struct {
  x10_async_handler_t handler;
  int niter;
} x10_agg_hdr_t;

static void
asyncSpawnCompHandlerAgg(lapi_handle_t *hndl, void *a)
{
  X10_DEBUG (1,  "Entry");

  x10_agg_cmpl_t* b = (x10_agg_cmpl_t*) a;
 
  //  if (__x10_my_place == 2) cout << "HI " << endl;

//  int tmp;

//  LAPI_Getcntr (__x10_hndl, &recvCntr[b->phase], &tmp);

//  assert (tmp == 0);

  LAPI_Setcntr (__x10_hndl, &recvCntr[b->phase], 1);
  
  recvMesgLen[b->phase] = b->len;

  assert (b->len < 2 * 16384 * 16);
  
  delete b;

  X10_DEBUG (1,  "Exit");
}


typedef struct {
  void* buf;
  x10_async_handler_t handler; 
  int niter;
} x10_agg_flush_cmpl_t;

static void
asyncFlushComplHandler (lapi_handle_t *hndl, void *a)
{
  X10_DEBUG (1,  "Entry");
  x10_agg_flush_cmpl_t *c = (x10_agg_flush_cmpl_t *)a;
  asyncSwitch(c->handler, (void *)(c->buf), c->niter);
  delete[] ((char*) c->buf);
  delete c;
  X10_DEBUG (1,  "Exit");
}


static void *
asyncFlushHandler(lapi_handle_t hndl, void *uhdr,
		     uint *uhdr_len, ulong *msg_len,
		     compl_hndlr_t **comp_h, void **user_info)
{
  X10_DEBUG (1,  "Entry");
  x10_agg_hdr_t buf = *((x10_agg_hdr_t *)uhdr);
  lapi_return_info_t *ret_info =
    (lapi_return_info_t *)msg_len;

  if (ret_info->udata_one_pkt_ptr || (*msg_len) == 0) {
    asyncSwitch(buf.handler, ret_info->udata_one_pkt_ptr,
		buf.niter);
    ret_info->ctl_flags = LAPI_BURY_MSG;
    *comp_h = NULL;
    X10_DEBUG (1,  "Exit");
    return NULL;  
  } else {
    x10_agg_flush_cmpl_t *c = new x10_agg_flush_cmpl_t;
    c->buf = (void *)new char [*msg_len];
    c->handler = buf.handler;
    c->niter = buf.niter;
    *comp_h = asyncFlushComplHandler;
    ret_info->ret_flags = LAPI_LOCAL_STATE;
    *user_info = (void *)c;
    X10_DEBUG (1,  "Exit");
    return c->buf;
  }

  X10_DEBUG (1,  "Exit");
  return NULL;
}


static void *
asyncSpawnHandlerAgg(lapi_handle_t hndl, void *uhdr,
		     uint *uhdr_len, ulong *msg_len,
		     compl_hndlr_t **comp_h, void **user_info)
{
  X10_DEBUG (1,  "Entry");

  lapi_return_info_t *ret_info =
    (lapi_return_info_t *)msg_len;
  
  x10_agg_cmpl_t* a = new x10_agg_cmpl_t;
  a->phase = *((ulong*) uhdr);
  a->len = *msg_len;
  *comp_h = asyncSpawnCompHandlerAgg;
  ret_info->ret_flags = LAPI_LOCAL_STATE;
  *user_info = (void*) a;

  X10_DEBUG (1,  "Exit");  
  return rbuf[a->phase];  
}

x10_err_t 
asyncAggInit_hc()
{
  X10_DEBUG (1,  "Entry");

  LRC(LAPI_Addr_set(__x10_hndl, (void *)asyncSpawnHandlerAgg, 8));
  
  for (int i = 0; i < X10_MAX_AGG_HANDLERS; i++) {
    __x10_agg_arg_buf[i] = new char* [__x10_num_places];
    __x10_agg_counter[i] = new int[__x10_num_places];
   
    for (int j = 0; j < __x10_num_places; j++) {
      __x10_agg_counter[i][j] = 0;      
      __x10_agg_arg_buf[i][j] = new char [X10_MAX_AGG_SIZE * 32 * sizeof(x10_async_arg_t)];
    }
  } 

  for (int i = 0; i < X10_MAX_LOG_NUMPROCS; i++) {
    rbuf[i] = new char [16384 * 32];
    LAPI_Setcntr (__x10_hndl, &(recvCntr[i]), 0);  
  }
  
  
  X10_DEBUG (1,  "Exit");
  return X10_OK;
}

x10_err_t
asyncAggFinalize_hc ()
{
  for (int i = 0; i < X10_MAX_AGG_HANDLERS; i++) {
     for (int j = 0; j < __x10_num_places; j++) {
       delete [] __x10_agg_arg_buf[i][j];
     }
     delete [] __x10_agg_arg_buf[i];
     delete [] __x10_agg_counter[i];
   } 
   
   return X10_OK;
}

static x10_err_t
sort_data_args (x10_async_handler_t hndlr,  int& ssize, size_t size, ulong mask, int phase, int cond)
{ 

  for (x10_place_t p = 0; p < __x10_num_places; p++) {
    
    if (p == __x10_my_place) continue;
    
    //    cout << "Hello " << __x10_my_place << " " << (p & mask) << " " << (MIN((((ulong) p) & mask), 1)) << " " <<  p << " " << __x10_agg_counter[hndlr][p] << endl;     
    if (((MIN((((ulong) p) & mask), 1)) == cond) && (__x10_agg_counter[hndlr][p] > 0)) {    
            
     
      memcpy (&(sbuf[ssize]), &p, sizeof(x10_place_t)); 
      ssize += sizeof(x10_place_t);
      
      //int message_size = __x10_agg_counter[hndlr][p] * size;
      memcpy (&(sbuf[ssize]), &__x10_agg_counter[hndlr][p], sizeof(int));  
      ssize += sizeof(int);
      
      memcpy (&(sbuf[ssize]), __x10_agg_arg_buf[hndlr][p], __x10_agg_counter[hndlr][p] * size);  
      ssize += __x10_agg_counter[hndlr][p] * size;
      
      __x10_agg_counter[hndlr][p] = 0;	    
      
    }
  }
}

static x10_err_t
sort_data_recvs (x10_async_handler_t hndlr, int& ssize, size_t size, ulong mask, int phase, int cond)
{ 
  for (int s = 0; s < recvMesgLen[phase]; ) {
    
    x10_place_t p =  *((x10_place_t*) (rbuf[phase] + s));
    s += sizeof (x10_place_t);

    int message_size = *((int *) (rbuf[phase] + s));    
    s += sizeof (int);
    
    //if (phase == 2) {cout << " P : " << p << endl; assert (p == __x10_my_place);}

    if (p == __x10_my_place) 
      {
	//int cntr = __x10_agg_counter[hndlr][p];
	asyncSwitch(hndlr, rbuf[phase] + s, message_size);
	//memcpy (&(__x10_agg_arg_buf[hndlr][__x10_my_place][cntr]), rbuf + s, message_size * size);
	//__x10_agg_counter[hndlr][p] += message_size;	
	
      } else if (((MIN((((ulong) p) & mask), 1)) == cond) && (message_size > 0)) {    
      
      assert (message_size > 0);
      
      memcpy (&(sbuf[ssize]), &p, sizeof(x10_place_t));       
      ssize += sizeof(x10_place_t);
	
      memcpy (&(sbuf[ssize]), &message_size, sizeof(int));        
      ssize += sizeof(int);
      
      memcpy (&(sbuf[ssize]), rbuf[phase] + s, message_size * size);        
      ssize += message_size * size;
    } else if (message_size > 0) {	
      int cntr = __x10_agg_counter[hndlr][p];	
      
      assert (cntr + message_size < 1024 * 16 * 2 * 8);
      
      memcpy (&(__x10_agg_arg_buf[hndlr][p][cntr * size]), rbuf[phase] + s, message_size * size);
      __x10_agg_counter[hndlr][p] += message_size;	
    }
    
    s += message_size * size;
  }
}

static x10_err_t
send_updates (int& ssize, int phase, int partner)
{ 
  lapi_cntr_t cntr;
  int tmp;

  //cout << "send " << phase << " " << __x10_my_place <<" " << partner << " " << *((int*) (sbuf)) << " " << *((int*) (sbuf + 4)) << " " << ssize << endl;       

  assert (ssize < 2 * 32 * 1024 * 8);

  ulong phase_l = (ulong) phase;
  //{ cout << "phase : " << phase << " " << sizeof(phase_l) << "  ssize: " << ssize << " p : " << __x10_my_place << " " << partner << endl; }
  LRC(LAPI_Setcntr(__x10_hndl, &cntr, 0));
  LRC(LAPI_Amsend(__x10_hndl, partner, (void *)8, &phase_l,
		  sizeof(phase_l),
		  (void *) sbuf,
		  ssize,
		  NULL, &cntr, 0));
  LRC(LAPI_Waitcntr(__x10_hndl, &cntr, 1, &tmp));

  ssize = 0;
}

static x10_err_t 
asyncSpawnInlineAgg_i (x10_place_t tgt,
		      x10_async_handler_t hndlr, size_t size)
{
  X10_DEBUG (1,  "Entry");
  
  __x10_agg_counter[hndlr][tgt]++;
  __x10_agg_total[hndlr]++;
 

  if ((__x10_agg_total[hndlr]+1) * size >=
      X10_MAX_AGG_SIZE * sizeof(x10_async_arg_t) ||
      (__x10_agg_total[hndlr]+1) >= X10_MAX_AGG_SIZE) {
    
    //LAPI_Gfence (__x10_hndl);              
    int factor = 1;
    int phase = 0;
    for (; factor < __x10_num_places; phase++, factor *= 2) {
      
      ulong partner = (1 << phase) ^ (ulong)__x10_my_place;
      ulong mask = ((ulong) 1) << phase;
      
      int tmp;
      
      int cond = partner > __x10_my_place ? 1 : 0;

      int ssize = 0;

      sort_data_args (hndlr, ssize, size, mask, phase, cond);
      
      if (phase > 0) {	
	//if (__x10_my_place == 2) cout << "{ " << endl;
	LAPI_Waitcntr (__x10_hndl, &(recvCntr[phase-1]), 1, &tmp);
	//if (__x10_my_place == 2) cout << "} " << endl;
	
	LAPI_Setcntr (__x10_hndl, &(recvCntr[phase-1]), 0);

	sort_data_recvs (hndlr, ssize, size, mask, phase-1, cond);	
	recvMesgLen[phase-1] = 0;

      }
      

      send_updates (ssize, phase, partner);  
      
      //      LAPI_Gfence (__x10_hndl);  
        
    }      

    int tmp;

    //    cout << "phase end " << phase << endl;

    LAPI_Waitcntr (__x10_hndl, &(recvCntr[phase-1]), 1, &tmp);
    
    LAPI_Setcntr (__x10_hndl, &(recvCntr[phase-1]), 0);
    
    int ssize = 0;
    sort_data_recvs (hndlr, ssize, size, 0, phase-1, 1);
    recvMesgLen[phase-1] = 0;

    __x10_agg_total[hndlr] = 0;

    
    asyncSwitch(hndlr, __x10_agg_arg_buf[hndlr][__x10_my_place], __x10_agg_counter[hndlr][__x10_my_place]);
    __x10_agg_counter[hndlr][__x10_my_place]=0;
    
     LAPI_Gfence (__x10_hndl);  
    
  }
  
  X10_DEBUG (1,  "Exit");
  return X10_OK;
}

namespace x10lib {

 x10_err_t asyncFlush_hc(x10_async_handler_t hndlr, size_t size)
  {
    X10_DEBUG (1,  "Entry");
    lapi_cntr_t cntr;
    int tmp;
    x10_agg_hdr_t buf;
    buf.handler = hndlr;
    
    //LAPI_Gfence (__x10_hndl);

    for (int j = 0; j < __x10_num_places; j++) {
      if (__x10_agg_counter[hndlr][j] > 0) {

	buf.niter = __x10_agg_counter[hndlr][j];

	LRC(LAPI_Setcntr(__x10_hndl, &cntr, 0));
	LRC(LAPI_Amsend(__x10_hndl, j, (void *) asyncFlushHandler, &buf,
			sizeof(x10_agg_hdr_t),
			(void *)__x10_agg_arg_buf[hndlr][j],
			size * __x10_agg_counter[hndlr][j],
			NULL, &cntr, NULL));
	LRC(LAPI_Waitcntr(__x10_hndl, &cntr, 1, &tmp));
      }
      __x10_agg_total[hndlr] -= __x10_agg_counter[hndlr][j];
      __x10_agg_counter[hndlr][j] = 0;
    }
    X10_DEBUG (1,  "Exit");
    return X10_OK;
  }


  x10_err_t
  asyncSpawnInlineAgg_hc(x10_place_t tgt, x10_async_handler_t hndlr,
		      void *args, size_t size)
  {
    X10_DEBUG (1,  "Entry");
    assert (size <= X10_MAX_AGG_SIZE * sizeof(x10_async_arg_t));
    int count = __x10_agg_counter[hndlr][tgt];
    memcpy(&(__x10_agg_arg_buf[hndlr][tgt][count * size]), args, size);
    x10_err_t err = asyncSpawnInlineAgg_i(tgt, hndlr, size);
    X10_DEBUG (1,  "Exit");
    return err;
  }

  x10_err_t
  asyncSpawnInlineAgg_hc (x10_place_t tgt, x10_async_handler_t hndlr,
		      x10_async_arg_t arg0)
  {
    X10_DEBUG (1,  "Entry");
    size_t size = sizeof(x10_async_arg_t);
    size_t count = __x10_agg_counter[hndlr][tgt];
    memcpy(&(__x10_agg_arg_buf[hndlr][tgt][count * size]),
	   &arg0, sizeof(x10_async_arg_t));
    x10_err_t err = asyncSpawnInlineAgg_i(tgt, hndlr, size);
    X10_DEBUG (1,  "Exit");
    return err;
  }

  x10_err_t
  asyncSpawnInlineAgg_hc(x10_place_t tgt, x10_async_handler_t hndlr,
		      x10_async_arg_t arg0, x10_async_arg_t arg1)
  {
    X10_DEBUG (1,  "Entry");
    size_t size = 2 * sizeof(x10_async_arg_t);
    int count = __x10_agg_counter[hndlr][tgt];

    memcpy(&(__x10_agg_arg_buf[hndlr][tgt][count * size]),
	   &arg0, sizeof(x10_async_arg_t));
    memcpy(&(__x10_agg_arg_buf[hndlr][tgt][count * size +
					   sizeof(x10_async_arg_t)]),
	   &arg1, sizeof(x10_async_arg_t));
    x10_err_t err = asyncSpawnInlineAgg_i(tgt, hndlr, size);
    X10_DEBUG (1,  "Exit");
    return err;
  }

} /* closing brace for namespace x10lib */



