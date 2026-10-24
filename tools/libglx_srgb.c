/* LD_PRELOAD shim for Minecraft 26.3 under Xvfb.
 * SDL3 / renderpearl ask for an sRGB GLX framebuffer. Xvfb's llvmpipe visuals
 * are sRGB=N, so glXChooseFBConfig fails and the client never opens a window.
 * LWJGL loads GLX with dlsym(handle), which skips a normal preload interpose,
 * so this wraps dlsym and remembers the real function from that handle.
 * Load it only for 26.3: the same preload trips glXGetProcAddress on the
 * GLFW-based 26.2 client.
 */
#define _GNU_SOURCE
#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define GLX_FRAMEBUFFER_SRGB_CAPABLE_ARB 0x20B2
#define GLX_NONE 0

static __thread int resolving;
static void *(*libc_dlsym)(void *, const char *) = NULL;
static void *(*real_choose)(void *, int, const int *, int *) = NULL;
static int (*real_attrib)(void *, void *, int, int *) = NULL;
static void *(*real_getproc)(const unsigned char *) = NULL;
static void *(*real_getproc_arb)(const unsigned char *) = NULL;

static void *next_dlsym(void *handle, const char *symbol) {
    if (libc_dlsym == NULL) {
        libc_dlsym = (void *(*)(void *, const char *))dlvsym(RTLD_NEXT, "dlsym", "GLIBC_2.34");
        if (libc_dlsym == NULL) {
            libc_dlsym = (void *(*)(void *, const char *))dlvsym(RTLD_NEXT, "dlsym", "GLIBC_2.2.5");
        }
    }
    if (libc_dlsym == NULL) {
        return NULL;
    }
    return libc_dlsym(handle, symbol);
}

void *glXChooseFBConfig(void *dpy, int screen, const int *attrib_list, int *nelements);
int glXGetFBConfigAttrib(void *dpy, void *config, int attribute, int *value);
void *glXGetProcAddress(const unsigned char *name);
void *glXGetProcAddressARB(const unsigned char *name);

static void remember(const char *symbol, void *real) {
    if (real == NULL || symbol == NULL) {
        return;
    }
    if (strcmp(symbol, "glXChooseFBConfig") == 0 && real_choose == NULL) {
        real_choose = real;
    } else if (strcmp(symbol, "glXGetFBConfigAttrib") == 0 && real_attrib == NULL) {
        real_attrib = real;
    } else if (strcmp(symbol, "glXGetProcAddress") == 0 && real_getproc == NULL) {
        real_getproc = real;
    } else if (strcmp(symbol, "glXGetProcAddressARB") == 0 && real_getproc_arb == NULL) {
        real_getproc_arb = real;
    }
}

static void *wrapper_for(const char *symbol) {
    if (symbol == NULL) {
        return NULL;
    }
    if (strcmp(symbol, "glXChooseFBConfig") == 0) {
        return (void *)glXChooseFBConfig;
    }
    if (strcmp(symbol, "glXGetFBConfigAttrib") == 0) {
        return (void *)glXGetFBConfigAttrib;
    }
    if (strcmp(symbol, "glXGetProcAddress") == 0) {
        return (void *)glXGetProcAddress;
    }
    if (strcmp(symbol, "glXGetProcAddressARB") == 0) {
        return (void *)glXGetProcAddressARB;
    }
    return NULL;
}

void *dlsym(void *handle, const char *symbol) {
    void *wrapped = wrapper_for(symbol);
    if (!resolving && wrapped != NULL) {
        resolving = 1;
        remember(symbol, next_dlsym(handle, symbol));
        resolving = 0;
        return wrapped;
    }
    return next_dlsym(handle, symbol);
}

static void *lookup_real(const char *symbol) {
    void *found;
    resolving = 1;
    found = next_dlsym(RTLD_NEXT, symbol);
    if (found == NULL) {
        found = next_dlsym(RTLD_DEFAULT, symbol);
    }
    resolving = 0;
    remember(symbol, found);
    return found;
}

static int *strip_srgb(const int *attribs) {
    int count;
    int *copy;
    int write;
    int read;
    if (attribs == NULL) {
        return NULL;
    }
    count = 0;
    while (attribs[count] != GLX_NONE) {
        count++;
        if (count > 256) {
            return NULL;
        }
    }
    copy = malloc((size_t)(count + 1) * sizeof(int));
    if (copy == NULL) {
        return NULL;
    }
    write = 0;
    read = 0;
    while (read < count) {
        if (attribs[read] == GLX_FRAMEBUFFER_SRGB_CAPABLE_ARB) {
            read += 2;
            continue;
        }
        copy[write++] = attribs[read++];
    }
    copy[write] = GLX_NONE;
    return copy;
}

void *glXChooseFBConfig(void *dpy, int screen, const int *attrib_list, int *nelements) {
    int *stripped;
    void *chosen;
    if (real_choose == NULL) {
        lookup_real("glXChooseFBConfig");
    }
    if (real_choose == NULL) {
        if (nelements != NULL) {
            *nelements = 0;
        }
        return NULL;
    }
    stripped = strip_srgb(attrib_list);
    chosen = real_choose(dpy, screen, stripped != NULL ? stripped : attrib_list, nelements);
    free(stripped);
    return chosen;
}

int glXGetFBConfigAttrib(void *dpy, void *config, int attribute, int *value) {
    if (real_attrib == NULL) {
        lookup_real("glXGetFBConfigAttrib");
    }
    if (attribute == GLX_FRAMEBUFFER_SRGB_CAPABLE_ARB && value != NULL) {
        *value = 1;
        return 0;
    }
    if (real_attrib == NULL) {
        return 1;
    }
    return real_attrib(dpy, config, attribute, value);
}

static void *resolve(const unsigned char *name) {
    void *wrapped = wrapper_for((const char *)name);
    void *(*real_get)(const unsigned char *) = real_getproc != NULL ? real_getproc : real_getproc_arb;
    if (wrapped != NULL) {
        return wrapped;
    }
    if (real_get == NULL) {
        lookup_real("glXGetProcAddress");
        real_get = real_getproc != NULL ? real_getproc : real_getproc_arb;
    }
    if (real_get == NULL) {
        return NULL;
    }
    return real_get(name);
}

void *glXGetProcAddress(const unsigned char *name) {
    return resolve(name);
}

void *glXGetProcAddressARB(const unsigned char *name) {
    return resolve(name);
}
